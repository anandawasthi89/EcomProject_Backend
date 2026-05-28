package com.project.ecomapp.ecommerce_Project.publisher;

import com.project.ecomapp.ecommerce_Project.Bean.BookCatalogResponse;

public class BookUploadPublisherHelper {
    public static BookUploadPayload getUploadPayload(BookCatalogResponse bookCatalogResponse){
        return new BookUploadPayload(bookCatalogResponse.title(), bookCatalogResponse.author(), bookCatalogResponse.publisher(), bookCatalogResponse.publishedDate().toString(), bookCatalogResponse.language(), bookCatalogResponse.pageCount());
    }
}
