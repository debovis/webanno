/*******************************************************************************
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.controller;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

/**
 * Utility Class for {@link TypeAdapter} with static methods such as geting
 * {@link TypeAdapter} based on its {@link CAS} {@link Type}
 *
 * @author Richard Eckart de Castilho
 * @author Seid Muhie Yimam
 */
public final class TypeUtil
{
	private TypeUtil() {
		// No instances
	}

    public static TypeAdapter getAdapter(AnnotationLayer aLayer)
    {
        if (aLayer.getType().equals(WebAnnoConst.SPAN_TYPE)) {
            SpanAdapter adapter = new SpanAdapter(aLayer);
            adapter.setLockToTokenOffsets(aLayer.isLockToTokenOffset());
            adapter.setAllowStacking(aLayer.isAllowStacking());
            adapter.setAllowMultipleToken(aLayer.isMultipleTokens());
            adapter.setCrossMultipleSentence(aLayer.isCrossSentence());
            return adapter;
        }
        else if (aLayer.getType().equals(WebAnnoConst.RELATION_TYPE)) {
            ArcAdapter adapter = new ArcAdapter(aLayer, aLayer.getId(), aLayer.getName(), "Dependent",
                    "Governor", aLayer.getAttachFeature() == null ? null : aLayer
                            .getAttachFeature().getName(), aLayer.getAttachType().getName());

            adapter.setCrossMultipleSentence(aLayer.isCrossSentence());
            adapter.setAllowStacking(aLayer.isAllowStacking());
            
            return adapter;
            // default is chain (based on operation, change to CoreferenceLinK)
        }
        else if (aLayer.getType().equals(WebAnnoConst.CHAIN_TYPE)) {
            ChainAdapter adapter = new ChainAdapter(aLayer, aLayer.getId(), aLayer.getName()
                    + ChainAdapter.CHAIN, aLayer.getName(), "first", "next");
            
            adapter.setLinkedListBehavior(aLayer.isLinkedListBehavior());
            
            return adapter;

        }
        else {
            throw new IllegalArgumentException("No adapter for type with name [" + aLayer.getName()
                    + "]");
        }
    }
    /**
     * Construct the label text used in the brat user interface.
     * 
     * @param aAdapter the adapter.
     * @param aFs the annotation.
     * @param aFeatures the features.
     * @return the label.
     */
    public static String getBratLabelText(TypeAdapter aAdapter, AnnotationFS aFs,
            List<AnnotationFeature> aFeatures)
    {
        StringBuilder bratLabelText = new StringBuilder();
        for (AnnotationFeature feature : aFeatures) {

            if (!(feature.isEnabled()) && !(feature.isVisible())) {
                continue;
            }

            Feature labelFeature = aFs.getType().getFeatureByBaseName(feature.getName());

            if (bratLabelText.length() > 0) {
                bratLabelText.append(TypeAdapter.FEATURE_SEPARATOR);
            }

            bratLabelText.append(StringUtils.defaultString(aFs
                    .getFeatureValueAsString(labelFeature)));
        }

        if (bratLabelText.length() > 0) {
            return bratLabelText.toString();
        }
        else {
            // If there are no label features at all, then use the layer UI name
            return "(" + aAdapter.getLayer().getUiName() + ")";
        }
    }

    /**
     * @param aBratTypeName the brat type name.
     * @return the layer ID.
     * @see #getBratTypeName
     */
    public static long getLayerId(String aBratTypeName)
    {
        return Long.parseLong(aBratTypeName.substring(0, aBratTypeName.indexOf("_")));
    }
    
    public static String getBratTypeName(TypeAdapter aAdapter)
    {
        return aAdapter.getTypeId() + "_" + aAdapter.getAnnotationTypeName();
    }

    public static String getBratTypeName(AnnotationLayer aLayer)
    {
        return aLayer.getId() + "_" + aLayer.getName();
    }
}
