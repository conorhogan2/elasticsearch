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
package org.elasticsearch.snapshots;

import com.google.common.collect.ImmutableList;
import org.elasticsearch.action.support.IgnoreIndices;
import org.elasticsearch.common.regex.Regex;
import org.elasticsearch.index.Index;
import org.elasticsearch.indices.IndexMissingException;

import java.util.HashSet;
import java.util.Set;

/**
 * Snapshot utilities
 */
public class SnapshotUtils {

    /**
     * Filters out list of available indices based on the list of selected indices.
     *
     * @param availableIndices list of available indices
     * @param selectedIndices  list of selected indices
     * @param ignoreIndices    ignore indices flag
     * @return filtered out indices
     */
    public static ImmutableList<String> filterIndices(ImmutableList<String> availableIndices, String[] selectedIndices, IgnoreIndices ignoreIndices) {
        if (selectedIndices == null || selectedIndices.length == 0) {
            return availableIndices;
        }
        Set<String> result = null;
        for (int i = 0; i < selectedIndices.length; i++) {
            String indexOrPattern = selectedIndices[i];
            boolean add = true;
            if (!indexOrPattern.isEmpty()) {
                if (availableIndices.contains(indexOrPattern)) {
                    if (result != null) {
                        result.add(indexOrPattern);
                    }
                    continue;
                }
                if (indexOrPattern.charAt(0) == '+') {
                    add = true;
                    indexOrPattern = indexOrPattern.substring(1);
                    // if its the first, add empty set
                    if (i == 0) {
                        result = new HashSet<String>();
                    }
                } else if (indexOrPattern.charAt(0) == '-') {
                    // if its the first, fill it with all the indices...
                    if (i == 0) {
                        result = new HashSet<String>(availableIndices);
                    }
                    add = false;
                    indexOrPattern = indexOrPattern.substring(1);
                }
            }
            if (indexOrPattern.isEmpty() || !Regex.isSimpleMatchPattern(indexOrPattern)) {
                if (!availableIndices.contains(indexOrPattern)) {
                    if (ignoreIndices != IgnoreIndices.MISSING) {
                        throw new IndexMissingException(new Index(indexOrPattern));
                    } else {
                        if (result == null) {
                            // add all the previous ones...
                            result = new HashSet<String>();
                            result.addAll(availableIndices.subList(0, i));
                        }
                    }
                } else {
                    if (result != null) {
                        if (add) {
                            result.add(indexOrPattern);
                        } else {
                            result.remove(indexOrPattern);
                        }
                    }
                }
                continue;
            }
            if (result == null) {
                // add all the previous ones...
                result = new HashSet<String>();
                result.addAll(availableIndices.subList(0, i));
            }
            boolean found = false;
            for (String index : availableIndices) {
                if (Regex.simpleMatch(indexOrPattern, index)) {
                    found = true;
                    if (add) {
                        result.add(index);
                    } else {
                        result.remove(index);
                    }
                }
            }
            if (!found && ignoreIndices != IgnoreIndices.MISSING) {
                throw new IndexMissingException(new Index(indexOrPattern));
            }
        }
        if (result == null) {
            return ImmutableList.copyOf(selectedIndices);
        }
        return ImmutableList.copyOf(result);
    }
}
