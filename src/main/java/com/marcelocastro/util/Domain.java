package com.marcelocastro.util;

public enum Domain {

  STUDENT("STD"),
  TEACHER("TCH"),
  STAFF("STF");

  private final String prefix;

  Domain(String prefix) {
    this.prefix = prefix;
  }

  public String getPrefix() {
    return prefix;
  }
  }
