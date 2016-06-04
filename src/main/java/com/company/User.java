package com.company;

import java.util.List;

public class User {
    String indexNumber;
    String factultyCode;
    List<Integer> registratiosnAvailable;

    public User(String indexNumber, String factultyCode, List<Integer> registratiosnAvailable) {
        this.indexNumber = indexNumber;
        this.factultyCode = factultyCode;
        this.registratiosnAvailable = registratiosnAvailable;
    }
}
