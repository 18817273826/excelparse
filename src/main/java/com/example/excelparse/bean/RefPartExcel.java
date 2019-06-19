package com.example.excelparse.bean;

import com.example.excelparse.annotation.ExcelColumn;
import lombok.Data;

@Data
public class RefPartExcel {
    @ExcelColumn("原零件号")
    private  String partNo;
    @ExcelColumn("原零件名称")
    private  String partNum;
    @ExcelColumn("参考零件号")
    private  String refPartNum;
    /*@ExcelColumn("长")
    private  String high;*/
    @ExcelColumn("宽")
    private  String width;
}
