package com.example.tdspring.dto;

import lombok.Data;
import java.util.Date;

@Data
public class MonthlyExcelFileDTO {
    private String filename;
    private String displayName;
    private Date lastUpdate;
    private int recordCount;
}
