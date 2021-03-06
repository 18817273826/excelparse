package com.example.excelparse.bean;

import com.example.excelparse.annotation.ExcelColumn;
import lombok.Data;

@Data
public class RefPartExcel {
    @ExcelColumn("原零件号123fxc")
    private  String partNo;
    @ExcelColumn("原零件名称1")
    private  String partNum;
    @ExcelColumn("参考零件号1")
    private  String refPartNum;
    /*@ExcelColumn("长")
    private  String high;*/
    @ExcelColumn("宽")
    private  String width;
}
