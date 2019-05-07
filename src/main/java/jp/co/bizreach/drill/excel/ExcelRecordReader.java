package jp.co.bizreach.drill.excel;

import io.netty.buffer.DrillBuf;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.impl.OutputMutator;
import org.apache.drill.exec.store.AbstractRecordReader;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.vector.complex.impl.VectorContainerWriter;
import org.apache.drill.exec.vector.complex.writer.BaseWriter;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;

public class ExcelRecordReader extends AbstractRecordReader {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ExcelRecordReader.class);
    private static final int MAX_RECORDS_PER_BATCH = 8096;

    private String inputPath;
    private DrillBuf buffer;
    private VectorContainerWriter writer;
    private Workbook wb;
    private Sheet sheet;
    private int rowNum;
    private int lastRowNum;
    private List<String> headers;
    private ExcelFormatPlugin.ExcelFormatConfig config;

    public ExcelRecordReader(FragmentContext fragmentContext, Path path, DrillFileSystem fileSystem,
                             List<SchemaPath> columns, ExcelFormatPlugin.ExcelFormatConfig config) throws OutOfMemoryException {
        try {
            this.inputPath = path.toUri().getPath();
            FSDataInputStream fsStream = fileSystem.open(path);

            this.wb = WorkbookFactory.create(fsStream);
            this.sheet = wb.getSheetAt(0);
            this.headers = getRowValues(sheet.getRow(sheet.getFirstRowNum()));
            this.rowNum = sheet.getFirstRowNum() + 1;
            this.lastRowNum = sheet.getLastRowNum();

            this.config = config;
            this.buffer = fragmentContext.getManagedBuffer();
            setColumns(columns);

        } catch(IOException e){
            logger.debug("Excel Plugin: " + e.getMessage());
        } catch(InvalidFormatException e){
            logger.debug("Excel Plugin: " + e.getMessage());
        }
    }

    public void setup(final OperatorContext context, final OutputMutator output) throws ExecutionSetupException {
        this.writer = new VectorContainerWriter(output);
    }

    public int next() {
        this.writer.allocate();
        this.writer.reset();

        int recordCount = 0;

        try {
            BaseWriter.MapWriter map = this.writer.rootAsMap();

            while(recordCount < MAX_RECORDS_PER_BATCH && rowNum <= lastRowNum){
                this.writer.setPosition(recordCount);
                map.start();

                List<String> values = getRowValues(sheet.getRow(rowNum));
                for(int i = 0; i < headers.size(); i++){
                    String fieldName = headers.get(i);
                    String fieldValue = values.get(i);
                    byte[] bytes = fieldValue.getBytes("UTF-8");
                    this.buffer.setBytes(0, bytes, 0, bytes.length);
                    map.varChar(fieldName).writeVarChar(0, bytes.length, buffer);
                }

                map.end();
                recordCount++;
                this.rowNum++;
            }

            this.writer.setValueCount(recordCount);
            return recordCount;

        } catch (final Exception e) {
            throw UserException.dataReadError(e).build(logger);
        }
    }

    public void close() throws Exception {
        this.wb.close();
    }

    private List<String> getRowValues(Row row){
        int firstCellNum = row.getFirstCellNum();
        int lastCellNum = row.getLastCellNum();
        List<String> values = new ArrayList<String>();
        for(int i = firstCellNum; i < lastCellNum; i++){
            Cell cell = row.getCell(i);
            CellType cellType = cell.getCellTypeEnum();
            switch(cellType){
                case BOOLEAN:
                    values.add(String.valueOf(cell.getBooleanCellValue()));
                    break;
                case NUMERIC:
                    values.add(String.valueOf(cell.getNumericCellValue()));
                    break;
                case BLANK:
                    values.add("");
                    break;
                case STRING:
                    values.add(cell.getStringCellValue());
                    break;
            }
        }
        return values;
    }

}
