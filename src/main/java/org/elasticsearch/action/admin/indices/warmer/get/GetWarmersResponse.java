/*
 * Licensed to ElasticSearch and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.action.admin.indices.warmer.get;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.search.warmer.IndexWarmersMetaData;

import java.io.IOException;
import java.util.Map;

/**
 */
public class GetWarmersResponse extends ActionResponse {

    private ImmutableMap<String, ImmutableList<IndexWarmersMetaData.Entry>> warmers = ImmutableMap.of();

    GetWarmersResponse(ImmutableMap<String, ImmutableList<IndexWarmersMetaData.Entry>> warmers) {
        this.warmers = warmers;
    }

    GetWarmersResponse() {
    }

    public ImmutableMap<String, ImmutableList<IndexWarmersMetaData.Entry>> warmers() {
        return warmers;
    }

    public ImmutableMap<String, ImmutableList<IndexWarmersMetaData.Entry>> getWarmers() {
        return warmers();
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        int size = in.readVInt();
        ImmutableMap.Builder<String, ImmutableList<IndexWarmersMetaData.Entry>> indexMapBuilder = ImmutableMap.builder();
        for (int i = 0; i < size; i++) {
            String key = in.readString();
            int valueSize = in.readVInt();
            ImmutableList.Builder<IndexWarmersMetaData.Entry> warmerEntryBuilder = ImmutableList.builder();
            for (int j = 0; j < valueSize; j++) {
                warmerEntryBuilder.add(new IndexWarmersMetaData.Entry(
                        in.readString(),
                        in.readStringArray(),
                        in.readBytesReference())
                );
            }
            indexMapBuilder.put(key, warmerEntryBuilder.build());
        }
        warmers = indexMapBuilder.build();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeVInt(warmers.size());
        for (Map.Entry<String, ImmutableList<IndexWarmersMetaData.Entry>> indexEntry : warmers.entrySet()) {
            out.writeString(indexEntry.getKey());
            out.writeVInt(indexEntry.getValue().size());
            for (IndexWarmersMetaData.Entry warmerEntry : indexEntry.getValue()) {
                out.writeString(warmerEntry.name());
                out.writeStringArray(warmerEntry.types());
                out.writeBytesReference(warmerEntry.source());
            }
        }
    }
}