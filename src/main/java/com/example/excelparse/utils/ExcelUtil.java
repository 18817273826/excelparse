package com.example.excelparse.utils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import com.example.excelparse.annotation.ExcelColumn;
import org.apache.commons.io.IOUtils;

import org.apache.commons.lang.time.FastDateFormat;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;


/**
 * 功能说明：Excel 导入/导出
 * 典型用法：无
 * 特殊用法：无
 * 创建者：phil
 * 创建时间： 2017年11月9日
 * 修改人：phil
 * 修改时间：
 * 修改原因：
 * 修改内容：
 * 版本号：1.0
 */
public class ExcelUtil {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0");// 格式化 number为整

    private static final DecimalFormat DECIMAL_FORMAT_PERCENT = new DecimalFormat("##.00%");//格式化分比格式，后面不足2位的用0补齐

//	private static final DecimalFormat df_per_ = new DecimalFormat("0.00%");//格式化分比格式，后面不足2位的用0补齐,比如0.00,%0.01%

//	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd"); // 格式化日期字符串

    private static final FastDateFormat FAST_DATE_FORMAT = FastDateFormat.getInstance("yyyy/MM/dd");

    private static final DecimalFormat DECIMAL_FORMAT_NUMBER  = new DecimalFormat("0.00E000"); //格式化科学计数器

    private static final Pattern POINTS_PATTERN = Pattern.compile("0.0+_*[^/s]+"); //小数匹配

    /**
     * 对外提供读取excel 的方法
     * @param file
     * @return
     * @throws IOException
     */
    public static List<List<Object>> readExcel(MultipartFile file) throws IOException {
        String extension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".") + 1).toLowerCase();
        if(Objects.equals("xls", extension) || Objects.equals("xlsx", extension)) {
            return readExcel(file.getInputStream());
        } else {
            throw new IOException("不支持的文件类型");
        }
    }

    /**
     * 对外提供读取excel 的方法
     * @param file
     * @param cls
     * @return
     * @throws IOException
     */
    public static <T> List<T> readExcel(MultipartFile file, Class<T> cls) throws IOException {
        String extension = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".") + 1).toLowerCase();
        if(Objects.equals("xls", extension) || Objects.equals("xlsx", extension)) {
            return readExcel(file.getInputStream(), cls);
        } else {
            throw new IOException("不支持的文件类型");
        }
    }

    /**
     * 读取 office excel
     *
     * @param stream
     * @return
     * @throws IOException
     */
    private static List<List<Object>> readExcel(InputStream inputStream) throws IOException {
        List<List<Object>> list = new LinkedList<>();
        Workbook workbook = null;
        try {
            workbook = WorkbookFactory.create(inputStream);
            int sheetsNumber = workbook.getNumberOfSheets();
            for (int n = 0; n < sheetsNumber; n++) {
                Sheet sheet = workbook.getSheetAt(n);
                Object value = null;
                Row row = null;
                Cell cell = null;
                for (int i = sheet.getFirstRowNum() + 1; i <= sheet.getPhysicalNumberOfRows(); i++) { // 从第二行开始读取
                    row = sheet.getRow(i);
                    if (StringUtils.isEmpty(row)) {
                        continue;
                    }
                    List<Object> linked = new LinkedList<>();
                    for (int j = row.getFirstCellNum(); j <= row.getLastCellNum(); j++) {
                        cell = row.getCell(j);
                        if (StringUtils.isEmpty(cell)) {
                            continue;
                        }
                        value = getCellValue(cell);
                        linked.add(value);
                    }
                    list.add(linked);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(workbook);
            IOUtils.closeQuietly(inputStream);
        }
        return list;
    }

    /**
     * 获取excel数据 将之转换成bean
     *
     * @param path
     * @param cls
     * @param <T>
     * @return
     */
    private static <T> List<T> readExcel(InputStream inputStream, Class<T> cls) {
        List<T> dataList = new LinkedList<T>();
        Workbook workbook = null;
        try {
            workbook = WorkbookFactory.create(inputStream);
            Map<String, List<Field>> classMap = new HashMap<String, List<Field>>();
            Field[] fields = cls.getDeclaredFields();
            for (Field field : fields) {
                ExcelColumn annotation = field.getAnnotation(ExcelColumn.class);
                if (annotation != null) {
                    String value = annotation.value();
                    if (!classMap.containsKey(value)) {
                        classMap.put(value, new ArrayList<Field>());
                    }
                    field.setAccessible(true);
                    classMap.get(value).add(field);
                }
            }
            Map<Integer, List<Field>> reflectionMap = new HashMap<Integer, List<Field>>();
            int sheetsNumber = workbook.getNumberOfSheets();
            for (int n = 0; n < sheetsNumber; n++) {
                Sheet sheet = workbook.getSheetAt(n);
                for (int j = sheet.getRow(0).getFirstCellNum(); j < sheet.getRow(0).getLastCellNum(); j++) { //首行提取注解
                    Object cellValue = getCellValue(sheet.getRow(0).getCell(j));
                    if (classMap.containsKey(cellValue)) {
                        reflectionMap.put(j, classMap.get(cellValue));
                    }
                }
                Row row = null;
                Cell cell = null;
                for (int i = sheet.getFirstRowNum() + 1; i < sheet.getPhysicalNumberOfRows(); i++) {
                    row = sheet.getRow(i);
                    T t = cls.newInstance();
                    for (int j = row.getFirstCellNum(); j < row.getLastCellNum(); j++) {
                        cell = row.getCell(j);
                        if (reflectionMap.containsKey(j)) {
                            Object cellValue = getCellValue(cell);
                            List<Field> fieldList = reflectionMap.get(j);
                            for (Field field : fieldList) {
                                try {
                                    field.set(t, cellValue);
                                } catch (Exception e) {
                                    //logger.error()
                                }
                            }
                        }
                    }
                    dataList.add(t);
                }
            }
        } catch (Exception e) {
            dataList = null;
        } finally {
            IOUtils.closeQuietly(workbook);
            IOUtils.closeQuietly(inputStream);
        }
        return dataList;
    }

    /**
     * 获取excel 单元格数据
     *
     * @param cell
     * @return
     */
    private static Object getCellValue(Cell cell) {
        Object value = null;
        switch (cell.getCellTypeEnum()) {
            case _NONE:
                break;
            case STRING:
                value = cell.getStringCellValue();
                break;
            case NUMERIC:
                if(DateUtil.isCellDateFormatted(cell)){ //日期
                    value = FAST_DATE_FORMAT.format(DateUtil.getJavaDate(cell.getNumericCellValue()));//统一转成 yyyy/MM/dd
                } else if("@".equals(cell.getCellStyle().getDataFormatString())
                        || "General".equals(cell.getCellStyle().getDataFormatString())
                        || "0_ ".equals(cell.getCellStyle().getDataFormatString())){
                    //文本  or 常规 or 整型数值
                    value = DECIMAL_FORMAT.format(cell.getNumericCellValue());
                } else if(POINTS_PATTERN.matcher(cell.getCellStyle().getDataFormatString()).matches()){ //正则匹配小数类型
                    value = cell.getNumericCellValue();  //直接显示
                } else if("0.00E+00".equals(cell.getCellStyle().getDataFormatString())){//科学计数
                    value = cell.getNumericCellValue();	//待完善
                    value = DECIMAL_FORMAT_NUMBER.format(value);
                } else if("0.00%".equals(cell.getCellStyle().getDataFormatString())){//百分比
                    value = cell.getNumericCellValue(); //待完善
                    value = DECIMAL_FORMAT_PERCENT.format(value);
                } else if("# ?/?".equals(cell.getCellStyle().getDataFormatString())){//分数
                    value = cell.getNumericCellValue(); ////待完善
                } else { //货币
                    value = cell.getNumericCellValue();
                    value = DecimalFormat.getCurrencyInstance().format(value);
                }
                break;
            case BOOLEAN:
                value = cell.getBooleanCellValue();
                break;
            case BLANK:
                //value = ",";
                break;
            default:
                value = cell.toString();
        }
        return value;
    }
}
