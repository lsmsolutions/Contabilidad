package com.silveira.accounting.application.business;

import com.silveira.accounting.models.business.Business;
import java.util.List;

public interface BusinessGateway {
    List<Business> findAll();
    long create(String name);
    void rename(long id, String name);
    void delete(long id);
}
