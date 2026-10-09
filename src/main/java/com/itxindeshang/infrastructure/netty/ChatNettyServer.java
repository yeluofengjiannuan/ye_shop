package com.itxindeshang.infrastructure.netty;

import com.itxindeshang.infrastructure.netty.handler.ChatHandler;
import com.itxindeshang.infrastructure.netty.handler.JwtAuthHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Netty 服务启动类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatNettyServer {

    @Value("${netty.port}")
    private int port;


    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    private final JwtAuthHandler jwtAuthHandler;
    private final ChatHandler chatHandler;

    @PostConstruct
    public void start() {
        new Thread(() -> {
            //创建两个线程组 boosGroup、workerGroup
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();
            try {
                //创建服务端的启动对象，设置参数
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workerGroup)
                        //设置服务端通道实现类型
                        .channel(NioServerSocketChannel.class)
                        //设置线程队列得到连接个数
                        .option(ChannelOption.SO_BACKLOG, 1024)
                        //设置子通道的参数
                        .childOption(ChannelOption.TCP_NODELAY, true)
                        //使用匿名内部类的形式初始化通道对象
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ChannelPipeline pipeline = ch.pipeline();
                                //给pipeline管道设置处理器
                                pipeline.addLast(new HttpServerCodec());
                                pipeline.addLast(new ChunkedWriteHandler());
                                pipeline.addLast(new HttpObjectAggregator(65536));
                                // 空闲状态检测：
                                //   readerIdle=120秒: 120秒没收到客户端数据（前端30秒心跳，留4倍容错）
                                //   writerIdle=0:     不检测写空闲（后端可能长时间不推消息）
                                //   allIdle=90秒:     90秒既没读也没写 → 绝对死连接，强制关
                                pipeline.addLast(new IdleStateHandler(120, 0, 90, TimeUnit.SECONDS));
                                // 1. 自定义鉴权处理器
                                pipeline.addLast(jwtAuthHandler);
                                // 2. 处理 WebSocket 握手 (关闭 extensions 以提高兼容性)
                                pipeline.addLast(new WebSocketServerProtocolHandler("/ws/chat", null, false, 65536));
                                // 3. 处理业务逻辑
                                pipeline.addLast(chatHandler);

                            }
                        });

                log.info("【Netty】服务已启动，监听端口: {}", port);
                //绑定端口号，启动服务端
                ChannelFuture f = b.bind(port).sync();
                //对关闭通道进行监听
                f.channel().closeFuture().sync();
            } catch (Exception e) {
                log.error("【Netty】服务异常: ", e);
            } finally {
                stop();
            }
        }).start();
    }

    /**
     * 关闭服务端
     */
    @PreDestroy
    public void stop() {
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
        log.info("【Netty】Netty Chat Server 已关闭");
    }
}