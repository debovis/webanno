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
package de.tudarmstadt.ukp.clarin.webanno.brat.ajax.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Test;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetCollectionInformationResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.project.PreferencesUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.tcf.TcfReader;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

/**
 * Test case for generating Brat Json data for getcollection and getcollection actions
 *
 * @author Seid M. Yimam
 *
 */

public class CasToBratJsonTest
    extends TestCase
{
    /*
     * @Resource(name = "annotationService") private AnnotationService annotationService;
     *
     * @Resource(name = "jsonConverter") private MappingJacksonHttpMessageConverter jsonConverter;
     */

    @Resource(name = "annotationService")
    private static AnnotationService annotationService;

    private Log LOG = LogFactory.getLog(getClass());

    /**
     * generate BRAT JSON for the collection informations
     *
     * @throws IOException if an I/O error occurs.
     */
    @Test
    public void testGenerateBratJsonGetCollection()
        throws IOException

    {
        MappingJacksonHttpMessageConverter jsonConverter = new MappingJacksonHttpMessageConverter();
        String jsonFilePath = "target/test-output/output_cas_to_json_collection.json";

        GetCollectionInformationResponse collectionInformation = new GetCollectionInformationResponse();

        List<AnnotationLayer> layerList = new ArrayList<AnnotationLayer>();

        AnnotationLayer layer = new AnnotationLayer();
        layer.setDescription("span annoattion");
        layer.setName("pos");
        layer.setType(WebAnnoConst.SPAN_TYPE);

        TagSet tagset = new TagSet();
        tagset.setDescription("pos");
        tagset.setLanguage("de");
        tagset.setName("STTS");

        Tag tag = new Tag();
        tag.setDescription("noun");
        tag.setName("NN");
        tag.setTagSet(tagset);

        layerList.add(layer);

        collectionInformation.addCollection("/Collection1/");
        collectionInformation.addCollection("/Collection2/");
        collectionInformation.addCollection("/Collection3/");

        collectionInformation.addDocument("/Collection1/doc1");
        collectionInformation.addDocument("/Collection2/doc1");
        collectionInformation.addDocument("/Collection3/doc1");
        collectionInformation.addDocument("/Collection1/doc2");
        collectionInformation.addDocument("/Collection2/doc2");
        collectionInformation.addDocument("/Collection3/doc2");

        collectionInformation.setSearchConfig(new ArrayList<String[]>());

        List<String> tagSetNames = new ArrayList<String>();
        tagSetNames.add(de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.POS);
        tagSetNames.add(de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.DEPENDENCY);
        tagSetNames.add(de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.NAMEDENTITY);
        tagSetNames.add(de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.COREFERENCE);
        tagSetNames
                .add(de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.COREFRELTYPE);

        JSONUtil.generateJson(jsonConverter, collectionInformation, new File(jsonFilePath));

        String reference = FileUtils.readFileToString(new File(
                "src/test/resources/output_cas_to_json_collection_expected.json"), "UTF-8");
        String actual = FileUtils.readFileToString(new File(
                "target/test-output/output_cas_to_json_collection.json"), "UTF-8");
        assertEquals(reference, actual);
    }

    /**
     * generate brat JSON data for the document
     */
    @Test
    public void testGenerateBratJsonGetDocument()
        throws Exception
    {
        MappingJacksonHttpMessageConverter jsonConverter = new MappingJacksonHttpMessageConverter();
        String jsonFilePath = "target/test-output/output_cas_to_json_document.json";

        InputStream is = null;
        JCas jCas = null;
        try {
            // is = new
            // FileInputStream("src/test/resources/tcf04-karin-wl.xml");
            String path = "src/test/resources/";
            String file = "tcf04-karin-wl.xml";
            CAS cas = JCasFactory.createJCas().getCas();
            CollectionReader reader = CollectionReaderFactory.createReader(
                    TcfReader.class, TcfReader.PARAM_SOURCE_LOCATION, path, TcfReader.PARAM_PATTERNS,
                    new String[] { "[+]" + file });
            if (!reader.hasNext()) {
                throw new FileNotFoundException("Annotation file [" + file + "] not found in ["
                        + path + "]");
            }
            reader.getNext(cas);
            jCas = cas.getJCas();

        }
        catch (FileNotFoundException ex) {
            LOG.info("The file specified not found " + ex.getCause());
        }
        catch (Exception ex) {
            LOG.info(ex);
        }

        List<String> tagSetNames = new ArrayList<String>();
        tagSetNames.add(de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.POS);
        tagSetNames.add(de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.DEPENDENCY);
        tagSetNames.add(de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.NAMEDENTITY);
        tagSetNames.add(de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.COREFERENCE);
        tagSetNames
                .add(de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.COREFRELTYPE);

        BratAnnotatorModel bratannotatorModel = new BratAnnotatorModel();
        bratannotatorModel.setWindowSize(10);
        bratannotatorModel.setSentenceAddress(BratAjaxCasUtil.getFirstSentenceAddress(jCas));
        bratannotatorModel.setLastSentenceAddress(BratAjaxCasUtil.getLastSentenceAddress(jCas));

        Sentence sentence = BratAjaxCasUtil.selectByAddr(jCas, Sentence.class,
                bratannotatorModel.getSentenceAddress());

        bratannotatorModel.setSentenceBeginOffset(sentence.getBegin());
        bratannotatorModel.setSentenceEndOffset(sentence.getEnd());

        Project project = new Project();
        bratannotatorModel.setProject(project);
        bratannotatorModel.setMode(Mode.ANNOTATION);

        GetDocumentResponse response = new GetDocumentResponse();
        response.setText(jCas.getDocumentText());

        SpanAdapter.renderTokenAndSentence(jCas, response, bratannotatorModel);

  /*      for (AnnotationLayer layer : bratannotatorModel.getAnnotationLayers()) {
            getAdapter(layer, annotationService).render(jCas,
                    annotationService.listAnnotationFeature(layer), response,
                    bratannotatorModel);
        }*/

        JSONUtil.generateJson(jsonConverter, response, new File(jsonFilePath));

        String reference = FileUtils.readFileToString(new File(
                "src/test/resources/output_cas_to_json_document_expected.json"), "UTF-8");
        String actual = FileUtils.readFileToString(new File(
                "target/test-output/output_cas_to_json_document.json"), "UTF-8");
        assertEquals(reference, actual);
    }
}
