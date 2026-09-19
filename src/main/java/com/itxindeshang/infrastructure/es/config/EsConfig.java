package com.itxindeshang.infrastructure.es.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import lombok.RequiredArgsConstructor;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@RequiredArgsConstructor
public class EsConfig {

    private final EsProperties esProperties;



    @Bean
    public ElasticsearchClient elasticsearchClient() {
        // 从 Spring 配置文件读取地址，自动解析 host 和 port
        String uris =esProperties.getUris();
        String address = uris.replace("http://", "");
        String host = address.split(":")[0];
        int port = Integer.parseInt(address.split(":")[1]);

        // 1. 配置用户名密码（从配置文件读取）
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(
                        esProperties.getUsername(),
                        esProperties.getPassword()
                )
        );

        // 2. 构建 RestClient，关键：注入认证信息
        RestClient restClient = RestClient.builder(new HttpHost(host, port, "http"))
                .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                )
                .build();

        //  LocalDateTime 序列化支持
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        RestClientTransport transport = new RestClientTransport(
                restClient,
                new JacksonJsonpMapper(objectMapper)
        );

        return new ElasticsearchClient(transport);
    }
}
