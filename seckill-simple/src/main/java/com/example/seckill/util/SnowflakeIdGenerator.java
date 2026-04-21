package com.example.seckill.util;

/**
 * 雪花算法生成分布式唯一ID
 * 结构：64 bit = 1(sign) + 41(timestamp) + 10(datacenterId+workerId) + 12(seq)
 * 时间戳精度: 毫秒（41位可用69年）
 * 支持 1024 个数据中心 + 工作进程，每秒可生成 409.6 万 ID
 */
public class SnowflakeIdGenerator {

    private static final long EPOCH = 1704067200000L; // 2024-01-01 00:00:00
    private static final int DATACENTER_BITS = 5;
    private static final int WORKER_BITS = 5;
    private static final int SEQUENCE_BITS = 12;

    private static final long DATACENTER_MASK = ~(-1L << DATACENTER_BITS);
    private static final long WORKER_MASK = ~(-1L << WORKER_BITS);
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private static final int WORKER_SHIFT = SEQUENCE_BITS;
    private static final int DATACENTER_SHIFT = SEQUENCE_BITS + WORKER_BITS;
    private static final int TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_BITS + DATACENTER_BITS;

    private final long datacenterId;
    private final long workerId;
    private volatile long lastTimestamp = -1L;
    private volatile long sequence = 0L;

    public SnowflakeIdGenerator(long datacenterId, long workerId) {
        if (datacenterId > DATACENTER_MASK || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId must be in [0, " + DATACENTER_MASK + "]");
        }
        if (workerId > WORKER_MASK || workerId < 0) {
            throw new IllegalArgumentException("workerId must be in [0, " + WORKER_MASK + "]");
        }
        this.datacenterId = datacenterId;
        this.workerId = workerId;
    }

    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock went backwards. Rejecting request for " + 
                    (lastTimestamp - timestamp) + "ms");
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_SHIFT)
                | (workerId << WORKER_SHIFT)
                | sequence;
    }

    private long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
