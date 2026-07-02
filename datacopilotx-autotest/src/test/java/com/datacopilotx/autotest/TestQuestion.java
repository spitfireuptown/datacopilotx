package com.datacopilotx.autotest;

import lombok.Data;

import java.util.List;

@Data
public class TestQuestion {
    private Integer id;
    private String question;
    private String gold_sql;
    private String difficulty;
    private List<String> tags;
}