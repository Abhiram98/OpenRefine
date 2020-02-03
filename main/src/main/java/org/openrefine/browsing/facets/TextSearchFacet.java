/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.openrefine.browsing.facets;

import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.browsing.FilteredRecords;
import org.openrefine.browsing.FilteredRows;
import org.openrefine.browsing.RecordFilter;
import org.openrefine.browsing.RowFilter;
import org.openrefine.browsing.facets.Facet;
import org.openrefine.browsing.facets.FacetConfig;
import org.openrefine.browsing.filters.AnyRowRecordFilter;
import org.openrefine.browsing.filters.ExpressionStringComparisonRowFilter;
import org.openrefine.expr.Evaluable;
import org.openrefine.grel.ast.VariableExpr;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.Project;
import org.openrefine.util.PatternSyntaxExceptionParser;

public class TextSearchFacet implements Facet {

    /*
     * Configuration
     */
    public static class TextSearchFacetConfig implements FacetConfig {

        @JsonProperty("name")
        protected String _name;
        @JsonProperty("columnName")
        protected String _columnName;
        @JsonProperty("query")
        protected String _query = null;
        @JsonProperty("mode")
        protected String _mode;
        @JsonProperty("caseSensitive")
        protected boolean _caseSensitive;
        @JsonProperty("invert")
        protected boolean _invert;

        @Override
        public TextSearchFacet apply(ColumnModel columnModel) {
            TextSearchFacet facet = new TextSearchFacet();
            facet.initializeFromConfig(this, columnModel);
            return facet;
        }

        @Override
        public String getJsonType() {
            return "core/text";
        }
    }

    TextSearchFacetConfig _config = new TextSearchFacetConfig();

    /*
     * Derived configuration
     */
    protected int _cellIndex;
    protected Pattern _pattern;
    protected String _query; // normalized version of the query from the config

    public TextSearchFacet() {
    }

    @JsonProperty("name")
    public String getName() {
        return _config._name;
    }

    @JsonProperty("columnName")
    public String getColumnName() {
        return _config._columnName;
    }

    @JsonProperty("query")
    public String getQuery() {
        return _config._query;
    }

    @JsonProperty("mode")
    public String getMode() {
        return _config._mode;
    }

    @JsonProperty("caseSensitive")
    public boolean isCaseSensitive() {
        return _config._caseSensitive;
    }

    @JsonProperty("invert")
    public boolean isInverted() {
        return _config._invert;
    }

    public void initializeFromConfig(TextSearchFacetConfig config, Project project) {
        _config = config;

        ColumnMetadata column = project.columnModel.getColumnByName(_config._columnName);
        _cellIndex = column != null ? column.getCellIndex() : -1;

        _query = _config._query;
        if (_query != null) {
            if ("regex".equals(_config._mode)) {
                try {
                    _pattern = Pattern.compile(
                            _query,
                            _config._caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
                } catch (java.util.regex.PatternSyntaxException e) {
                    PatternSyntaxExceptionParser err = new PatternSyntaxExceptionParser(e);
                    throw new IllegalArgumentException(err.getUserMessage());
                }
            } else if (!_config._caseSensitive) {
                _query = _query.toLowerCase();
            }
        }

    }

    @Override
    public RowFilter getRowFilter(ColumnModel columnModel) {
        if (_query == null || _query.length() == 0 || _cellIndex < 0) {
            return null;
        } else if ("regex".equals(_config._mode) && _pattern == null) {
            return null;
        }

        Evaluable eval = new VariableExpr("value");

        if ("regex".equals(_config._mode)) {
            return new ExpressionStringComparisonRowFilter(eval, _config._invert, _config._columnName, _cellIndex) {

                @Override
                protected boolean checkValue(String s) {
                    return _pattern.matcher(s).find();
                };
            };
        } else {
            return new ExpressionStringComparisonRowFilter(eval, _config._invert, _config._columnName, _cellIndex) {

                @Override
                protected boolean checkValue(String s) {
                    return (_config._caseSensitive ? s : s.toLowerCase()).contains(_query);
                };
            };
        }
    }

    @Override
    public RecordFilter getRecordFilter(ColumnModel columnModel) {
        RowFilter rowFilter = getRowFilter(columnModel);
        return rowFilter == null ? null : new AnyRowRecordFilter(rowFilter);
    }

    @Override
    public void computeChoices(Project project, FilteredRows filteredRows) {
        // nothing to do
    }

    @Override
    public void computeChoices(Project project, FilteredRecords filteredRecords) {
        // nothing to do
    }
}