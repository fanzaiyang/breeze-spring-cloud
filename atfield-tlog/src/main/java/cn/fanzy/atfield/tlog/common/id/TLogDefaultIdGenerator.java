package cn.fanzy.atfield.tlog.common.id;


import cn.fanzy.atfield.tlog.common.id.snowflake.UniqueIdGenerator;

public class TLogDefaultIdGenerator extends TLogIdGenerator {
    @Override
    public String generateTraceId() {
        return UniqueIdGenerator.generateStringId();
    }

    public static void main(String[] args) {
        System.out.println();
    }
}
