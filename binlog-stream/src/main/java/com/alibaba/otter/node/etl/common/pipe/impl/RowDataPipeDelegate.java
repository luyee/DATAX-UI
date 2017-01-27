/*
 * Copyright (C) 2010-2101 Alibaba Group Holding Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.otter.node.etl.common.pipe.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.springframework.util.Assert;

import com.alibaba.otter.node.common.config.ConfigClientService;
import com.alibaba.otter.node.etl.common.pipe.PipeKey;
import com.alibaba.otter.node.etl.common.pipe.exception.PipeException;
import com.alibaba.otter.node.etl.common.pipe.impl.memory.MemoryPipeKey;
import com.alibaba.otter.node.etl.common.pipe.impl.memory.RowDataMemoryPipe;
import com.alibaba.otter.node.etl.common.pipe.impl.rpc.RowDataRpcPipe;
import com.alibaba.otter.node.etl.common.pipe.impl.rpc.RpcPipeKey;
import com.alibaba.otter.shared.common.model.config.pipeline.Pipeline;
import com.alibaba.otter.shared.common.model.config.pipeline.PipelineParameter.PipeChooseMode;
import com.alibaba.otter.shared.etl.model.DbBatch;
import com.alibaba.otter.shared.etl.model.EventData;

/**
 * 管道操作相关工具类
 * 
 * @author jianghang 2011-10-17 下午04:02:35
 * @version 4.0.0
 */
public class RowDataPipeDelegate {

    private RowDataMemoryPipe   rowDataMemoryPipe;
    private RowDataRpcPipe      rowDataRpcPipe;
    private ConfigClientService configClientService;
    private long                sizeThresold = 1024 * 128L; // 默认1MB

    /**
     * 将对应的数据传递到指定的Node id节点上
     */
    public List<PipeKey> put(final DbBatch data, Long nid) throws PipeException {
        List<PipeKey> keys = new ArrayList<PipeKey>();
        if (isLocal(nid)) {
            keys.add(rowDataMemoryPipe.put(data));
        } else {
            Pipeline pipeline = configClientService.findPipeline(data.getRowBatch().getIdentity().getPipelineId());
            try {
                PipeChooseMode pipeChooseMode = pipeline.getParameters().getPipeChooseType();
                if (pipeChooseMode.isAutomatic()) {
                    if (calculateSize(data) <= sizeThresold) {
                        keys.add(rowDataRpcPipe.put(data));
                    } 
                } else if (pipeChooseMode.isRpc()) {
                    keys.add(rowDataRpcPipe.put(data));
                } else {
                    throw new PipeException("pipeChooseMode is error!" + pipeChooseMode);
                }
            } catch (Exception e) {
                throw new PipeException(e);
            }
        }

        return keys;
    }

    public DbBatch get(List<PipeKey> keys) {
        Assert.notNull(keys);
        DbBatch dbBatch = new DbBatch();
        for (final PipeKey key : keys) {
            if (key == null) {
                continue; // 忽略空的key
            }

            if (key instanceof MemoryPipeKey) {
                dbBatch = rowDataMemoryPipe.get((MemoryPipeKey) key);
                return dbBatch;// 直接返回
            }  else if (key instanceof RpcPipeKey) {
                dbBatch = rowDataRpcPipe.get((RpcPipeKey) key);
            } else {
                throw new PipeException("unknow_PipeKey", key.toString());
            }
        }
        return dbBatch;
    }

    // 大致估算一下row记录的大小
    private long calculateSize(DbBatch data) {
        long size = 0;
        for (EventData eventData : data.getRowBatch().getDatas()) {
            size += eventData.getSize();
        }

        return size;

        // 走序列化的方式快速计算一下大小
        // SerializeWriter out = new SerializeWriter();
        // try {
        // JSONSerializer serializer = new JSONSerializer(out);
        // serializer.config(SerializerFeature.SortField, false);// 关掉排序
        // serializer.write(data);
        // byte[] bytes = out.toBytes("UTF-8");
        // return bytes.length;
        // } finally {
        // out.close();
        // }
    }

    private boolean isLocal(Long nid) {
        return configClientService.currentNode().getId().equals(nid);
    }

    // ================ setter / getter ===============

    public void setRowDataMemoryPipe(RowDataMemoryPipe rowDataMemoryPipe) {
        this.rowDataMemoryPipe = rowDataMemoryPipe;
    }


    public void setRowDataRpcPipe(RowDataRpcPipe rowDataRpcPipe) {
        this.rowDataRpcPipe = rowDataRpcPipe;
    }

    public void setConfigClientService(ConfigClientService configClientService) {
        this.configClientService = configClientService;
    }


    public void setSizeThresold(long sizeThresold) {
        this.sizeThresold = sizeThresold;
    }

}