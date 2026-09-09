package com.example.tdspring.dto;  // ← CORRIGER ICI (pas com.ponet.optibox.dto)

import lombok.Data;
import java.util.Date;

@Data
public class CheckRecordDTO {
    private String alitracer;
    private String size;
    private String cmu;
    /** "Casier X" si l'outil est dans un casier, sinon le nom de la zone atelier (ex: "Maintenance"). */
    private String location;
    private int status;
    private String comment;
    private String controlledBy;
    private Date date;
}
