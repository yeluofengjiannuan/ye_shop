package com.itxindeshang.infrastructure.es.service;

import com.itxindeshang.infrastructure.es.document.ProductDocument;
import com.itxindeshang.pojo.enums.CommonStatus;

public interface ProductDocumentService {

    void save(ProductDocument doc);

    void updateStatus(Long productId, CommonStatus status);
}
