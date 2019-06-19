package com.example.excelparse.controller;

import com.example.excelparse.bean.RefPartExcel;
import com.example.excelparse.utils.ExcelUtil;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class ExcelParse {
    @RequestMapping("/upload")
    public String excelParse(@RequestParam("file") MultipartFile file, HttpServletRequest request){
        //Map<String, Object> map = new HashMap<String, Object>();
        // 判断文件是否为空
        if (!StringUtils.isEmpty(file)) {
            try {
                List<RefPartExcel> excelBeans = ExcelUtil.readExcel(file,RefPartExcel.class);
                System.out.println(excelBeans.size());
                for(RefPartExcel ep : excelBeans){
                    System.out.println(ep.toString());
                }
                //........逻辑
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "1";
    }

}
