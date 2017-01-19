package jp.co.bizreach.drill.excel;

import com.google.common.collect.ImmutableList;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.store.RecordReader;
import org.apache.drill.exec.store.RecordWriter;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.store.dfs.easy.EasyFormatPlugin;
import org.apache.drill.exec.store.dfs.easy.EasyWriter;
import org.apache.drill.exec.store.dfs.easy.FileWork;
import org.apache.hadoop.conf.Configuration;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.IOException;
import java.util.List;

public class ExcelFormatPlugin extends EasyFormatPlugin<ExcelFormatPlugin.ExcelFormatConfig> {

    private static final boolean IS_COMPRESSIBLE = false;
    private static final String DEFAULT_NAME = "excel";
    private ExcelFormatConfig config;

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ExcelFormatPlugin.class);

    public ExcelFormatPlugin(String name, DrillbitContext context, Configuration fsConf, StoragePluginConfig storageConfig) {
        this(name, context, fsConf, storageConfig, new ExcelFormatConfig());
    }

    public ExcelFormatPlugin(String name, DrillbitContext context, Configuration fsConf, StoragePluginConfig config, ExcelFormatConfig formatPluginConfig) {
        super(name, context, fsConf, config, formatPluginConfig, true, false, false, IS_COMPRESSIBLE, formatPluginConfig.getExtensions(), DEFAULT_NAME);
        this.config = formatPluginConfig;
    }

    @Override
    public RecordReader getRecordReader(FragmentContext context, DrillFileSystem dfs, FileWork fileWork,
                                        List<SchemaPath> columns, String userName) throws ExecutionSetupException {
        return new ExcelRecordReader(context, fileWork.getPath(), dfs, columns, config);
    }


    @Override
    public int getReaderOperatorType() {
        // TODO Is it correct??
        return UserBitShared.CoreOperatorType.JSON_SUB_SCAN_VALUE;
    }

    @Override
    public int getWriterOperatorType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean supportsPushDown() {
        return true;
    }

    @Override
    public RecordWriter getRecordWriter(FragmentContext context, EasyWriter writer) throws IOException {
        return null;
    }

    @JsonTypeName("excel")
    public static class ExcelFormatConfig implements FormatPluginConfig {
        public List<String> extensions;

        private static final List<String> DEFAULT_EXTS = ImmutableList.of("xlsx");

        @JsonInclude(JsonInclude.Include.NON_DEFAULT)
        public List<String> getExtensions() {
            if (extensions == null) {
                // when loading an old JSONFormatConfig that doesn't contain an "extensions" attribute
                return DEFAULT_EXTS;
            }
            return extensions;
        }

        @Override
        public int hashCode() {
            return 99;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (obj == null) {
                return false;
            } else if (getClass() == obj.getClass()) {
                return true;
            }
            return false;
        }
    }

}
