package org.ff4j.utils;

/*
 * #%L FeatureJsonMarshaller.java (ff4j-web) by Cedrick LUNVEN %% Copyright (C) 2013 Ff4J %% Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License. #L%
 */

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.ff4j.core.Feature;
import org.ff4j.core.FlipStrategy;

/**
 * Utility class to produce JSON.
 * 
 * @author <a href="mailto:cedrick.lunven@gmail.com">Cedrick LUNVEN</a>
 */
public final class FeatureJsonMarshaller {

    /** Null expression for JSON. */
    private static final String NULL_JSON = "null";

    /**
     * Hide default constructor from utility class.
     */
    private FeatureJsonMarshaller() {}

    /**
     * Marshall Feature to JSON.
     * 
     * @param feature
     *            target feature
     * @return jsn output
     */
    public static String marshallFeature(Feature feature) {
        if (feature == null) {
            return NULL_JSON;
        }
        StringBuilder jsonOutput = new StringBuilder("{");
        jsonOutput.append(renderAttributeString("uid", feature.getUid()));
        jsonOutput.append(",");
        jsonOutput.append(renderAttributeBoolean("enable", feature.isEnable()));
        jsonOutput.append(",");
        jsonOutput.append(renderAttributeString("description", feature.getDescription()));
        jsonOutput.append(",");
        jsonOutput.append(renderAttributeString("group", feature.getGroup()));
        // Authorizations
        jsonOutput.append(renderAuthorizations(feature.getAuthorizations()));
        // Flipping strategy
        jsonOutput.append(renderFlippingStrategy(feature.getFlippingStrategy()));
        jsonOutput.append("}");
        return jsonOutput.toString();
    }

    /**
     * Create String from Feature.
     * 
     * @param str
     *            feature as String
     * @return feature object
     */
    public static Feature unMarshallFeature(String str) {
        // 1 - remove bracket
        String expected = str.substring(1, str.length() - 1);
        // 2 - isolated flipstrategy
        String[] fs = expected.split(",\"flippingStrategy\":");
        // 3 - isolated authorization
        String[] auths = fs[0].split(",\"authorizations\":");
        // 4 - Parse
        expected = auths[0];
        Map<String, String> mapOfAttributes = parseJsonAttributes(expected.split(","));
        assertField(mapOfAttributes, expected, "uid");
        Feature targetFeature = new Feature(mapOfAttributes.get("uid"));
        assertField(mapOfAttributes, expected, "enable");
        targetFeature.setEnable(Boolean.valueOf(mapOfAttributes.get("enable")));
        assertField(mapOfAttributes, expected, "description");
        targetFeature.setDescription(mapOfAttributes.get("description"));
        assertField(mapOfAttributes, expected, "group");
        targetFeature.setGroup(mapOfAttributes.get("group"));
        // Strategy
        targetFeature.setFlippingStrategy(parseFlipStrategy(targetFeature.getUid(), fs[1]));
        // Authorizations
        targetFeature.setAuthorizations(parseAuthorizations(auths[1]));
        
        return targetFeature;
    }

    /**
     * @param params
     * @param initString
     * @param paramName
     */
    private static void assertField(Map<String, String> params, String initString, String paramName) {
        if (!params.containsKey(paramName)) {
            throw new IllegalArgumentException(String.format("Cannot parse %s '%s' is expected but not found", initString,
                    paramName));
        }
    }

    /**
     * Extract value from "name":"value" expressions (JSON).
     * 
     * @return value
     */
    private static Map<String, String> parseJsonAttributes(String[] exprs) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        if (exprs != null) {
            for (String exp : exprs) {
                String name = exp.substring(0, exp.indexOf(":")).replaceAll("\"", "");
                String val = exp.substring(exp.indexOf(":") + 1).replaceAll("\"", "");
                if ("null".equals(val)) {
                    val = null;
                }
                map.put(name, val);
            }
        }
        return map;
    }

    /**
     * Parse FlipStrategy JSON expression.
     * 
     * @param featureName
     *            current featurename
     * @param str
     *            flipstrategy as a string
     * @return flip strategy as an object
     */
    private static FlipStrategy parseFlipStrategy(String featureName, String str) {
        if ("null".equals(str)) {
            return null;
        }

        FlipStrategy strategy = null;
        String expected = str.substring(1, str.length() - 1);
        String classType = expected.substring(expected.lastIndexOf(":") + 1).replaceAll("\"", "");

        try {
            strategy = (FlipStrategy) Class.forName(classType).newInstance();
        } catch (InstantiationException e) {
            throw new IllegalArgumentException(classType + " does not seems to have a DEFAULT constructor", e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(classType + " does not seems to have a PUBLIC constructor", e);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(classType + " has not been found within classpath, check syntax", e);
        }

        String initParams = expected.substring(14, expected.lastIndexOf(",") - 1);
        strategy.init(featureName, parseJsonAttributes(initParams.split(",")));
        return strategy;
    }

    /**
     * Parse Authorization if exist
     * 
     * @param expr
     *            authorization expression
     * @return set of authorizations
     */
    private static Set<String> parseAuthorizations(String expr) {
        Set<String> auths = new HashSet<String>();
        String exprTmp = expr.substring(1, expr.length() - 1).replaceAll("\"", "");
        if (!"".equals(exprTmp)) {
            auths.addAll(Arrays.asList(exprTmp.split(",")));
        }
        return auths;
    }

    /**
     * Render {@link FlipStrategy} as a Json string.
     * 
     * @param strat
     *            flipping strategy
     * @return json string
     */
    private static String renderFlippingStrategy(FlipStrategy strat) {
        StringBuilder jsonOutput = new StringBuilder(",\"flippingStrategy\":");
        if (strat == null) {
            jsonOutput.append(NULL_JSON);
        } else {
            jsonOutput.append("{\"initParams\":{");
            Map < String , String> iparams = strat.getInitParams();
            if (iparams != null && !iparams.isEmpty()) {
                boolean first = true;
                for (Entry<String, String> param : iparams.entrySet()) {
                    if (!first) {
                        jsonOutput.append(",");
                    }
                    jsonOutput.append("\"" + param.getKey() + "\":\"" + param.getValue() + "\"");
                    first = false;
                }
            }
            jsonOutput.append("},\"classType\":\"");
            jsonOutput.append(strat.getClass().getCanonicalName());
            jsonOutput.append("\"}");
        }
        return jsonOutput.toString();
    }

    /**
     * Render authorizations Set as a Json string.
     * 
     * @param auths
     *            authorizations set
     * @return json string
     */
    private static String renderAuthorizations(Set<String> auths) {
        StringBuilder jsonOutput = new StringBuilder(",\"authorizations\":");
        if (auths == null) {
            jsonOutput.append(NULL_JSON);
        } else {
            // Even if empty list returning squared brackets
            jsonOutput.append("[");
            if (!auths.isEmpty()) {
                boolean first = true;
                for (String auth : auths) {
                    if (first) {
                        first = false;
                    } else {
                        jsonOutput.append(",");
                    }
                    jsonOutput.append("\"");
                    jsonOutput.append(auth);
                    jsonOutput.append("\"");
                }
            }
            jsonOutput.append("]");
        }
        return jsonOutput.toString();
    }

    /**
     * Render Sample String attribute.
     * 
     * @param name
     *            attribute name
     * @param value
     *            attribute value
     * @return json expression for this attribute
     */
    private static String renderAttributeString(String name, String value) {
        StringBuilder jsonOutput = new StringBuilder("\"");
        jsonOutput.append(name);
        jsonOutput.append("\":");
        if (value == null) {
            jsonOutput.append(NULL_JSON);
        } else {
            jsonOutput.append("\"");
            jsonOutput.append(value);
            jsonOutput.append("\"");
        }
        return jsonOutput.toString();
    }

    /**
     * Render Sample String attribute.
     * 
     * @param name
     *            attribute name
     * @param value
     *            attribute value
     * @return json expression for this attribute
     */
    private static String renderAttributeBoolean(String name, boolean value) {
        StringBuilder jsonOutput = new StringBuilder("\"");
        jsonOutput.append(name);
        jsonOutput.append("\":");
        jsonOutput.append(value);
        return jsonOutput.toString();
    }

}