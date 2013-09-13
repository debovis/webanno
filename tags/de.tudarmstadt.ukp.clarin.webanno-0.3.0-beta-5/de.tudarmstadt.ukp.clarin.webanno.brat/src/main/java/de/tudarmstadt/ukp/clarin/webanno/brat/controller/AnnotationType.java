/*******************************************************************************
 * Copyright 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.controller;

/**
 * Constants for annotation types
 * @author Seid Muhie Yimam
 *
 */
public class AnnotationType
{
    // Annotation types, for span or arc annotations.
    public static final String POS = "pos";
    public static final String NAMEDENTITY = "named entity";
    public static final String DEPENDENCY = "dependency";
    public static final String COREFERENCE = "coreference";
    public static final String COREFRELTYPE = "coreference type";
    public static final String LEMMA = "lemma";

    // USed for span annotation hierarchy, as parent name.
    // This is, in brat terminology, unused entity type.
    public static final String POS_PREFIX = "POS_";
    public static final String NAMEDENTITY_PREFIX = "Named Entity_";
    public static final String COREFERENCE_PREFIX = "COREF_";

    public static final String PREFIX = "_";



    // USed for span annotation hierarchy, as parent name.
    // This is, in brat terminology, unused entity type.

    public static final String POS_PARENT = "POS";
    public static final String NAMEDENTITY_PARENT = "Named Entity";
    public static final String COREFERENCE_PARENT = "COREF";

}