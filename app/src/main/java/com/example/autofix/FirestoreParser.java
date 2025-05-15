package com.example.autofix;

import com.example.autofix.models.Service;
import com.example.autofix.models.ServiceCategory;
import com.example.autofix.models.ServiceSubcategory;
import com.google.firebase.firestore.DocumentSnapshot;

public class FirestoreParser {
    public static ServiceCategory parseCategory(DocumentSnapshot document) {
        ServiceCategory category = document.toObject(ServiceCategory.class);
        if (category != null) {
            category.setId(document.getId());
        }
        return category;
    }

    public static ServiceSubcategory parseSubcategory(DocumentSnapshot document) {
        ServiceSubcategory subcategory = document.toObject(ServiceSubcategory.class);
        if (subcategory != null) {
            subcategory.setId(document.getId());
        }
        return subcategory;
    }

    public static Service parseService(DocumentSnapshot document) {
        Service service = document.toObject(Service.class);
        if (service != null) {
            service.setId(document.getId());
        }
        return service;
    }
}