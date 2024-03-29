/*******************************************************************************
 * Copyright 2013
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
package de.tudarmstadt.ukp.clarin.webanno.monitoring.statistics;

import static org.apache.uima.fit.factory.CollectionReaderFactory.createCollectionReader;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.brat.controller.ArcAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.monitoring.page.MonitoringPage;
import de.tudarmstadt.ukp.clarin.webanno.tcf.TcfReader;
import de.tudarmstadt.ukp.clarin.webanno.tsv.WebannoCustomTsvReader;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/**
 * Unit Test for Kappa Agreement. The example reads two TSV files with POS and DEP annotations for
 * two users and check the disagreement
 *
 * @author yimam
 *
 */
public class TwoPairedKappaTest
{
    private User user1, user2, user3;
    private SourceDocument document;
    private CAS cas1, cas2, cas3, cas4;

    private void init()
        throws Exception
    {

        user1 = new User();
        user1.setUsername("user1");

        user2 = new User();
        user2.setUsername("user2");

        user3 = new User();
        user3.setUsername("user3");

        document = new SourceDocument();

        cas1 = JCasFactory.createJCas().getCas();
        CollectionReader reader1 = createCollectionReader(WebannoCustomTsvReader.class,
                WebannoCustomTsvReader.PARAM_PATH, new File("src/test/resources/").getAbsolutePath(),
                TcfReader.PARAM_PATTERNS, new String[] { "[+]kappatest.tsv" });
        reader1.getNext(cas1);

        cas2 = JCasFactory.createJCas().getCas();
        CollectionReader reader2 = createCollectionReader(WebannoCustomTsvReader.class,
                WebannoCustomTsvReader.PARAM_PATH, new File("src/test/resources/").getAbsolutePath(),
                TcfReader.PARAM_PATTERNS, new String[] { "[+]kappaspandiff.tsv" });
        reader2.getNext(cas2);

        cas3 = JCasFactory.createJCas().getCas();
        CollectionReader reader3 = createCollectionReader(WebannoCustomTsvReader.class,
                WebannoCustomTsvReader.PARAM_PATH, new File("src/test/resources/").getAbsolutePath(),
                TcfReader.PARAM_PATTERNS, new String[] { "[+]kappaarcdiff.tsv" });
        reader3.getNext(cas3);

        cas4 = JCasFactory.createJCas().getCas();
        CollectionReader reader4 = createCollectionReader(WebannoCustomTsvReader.class,
                WebannoCustomTsvReader.PARAM_PATH, new File("src/test/resources/").getAbsolutePath(),
                TcfReader.PARAM_PATTERNS, new String[] { "[+]kappaspanarcdiff.tsv" });
        reader4.getNext(cas4);

    }

    @Test
    public void testTwoUserSameAnnotation()
        throws Exception
    {
        init();
        double[][] results = new double[2][2];
        AnnotationLayer layer = new AnnotationLayer();
        layer.setId(0);
        layer.setName(POS.class.getName());
        TypeAdapter adapter = new SpanAdapter(layer);
        Map<User, List<SourceDocument>> userDocs = new HashMap<User, List<SourceDocument>>();
        userDocs.put(user1, Arrays.asList(new SourceDocument[] { document }));
        userDocs.put(user2, Arrays.asList(new SourceDocument[] { document }));
        Map<SourceDocument, Map<User, JCas>> documentJCases = new HashMap<SourceDocument, Map<User, JCas>>();
        Map<User, JCas> userCases = new HashMap<User, JCas>();
        userCases.put(user1, cas1.getJCas());
        userCases.put(user2, cas1.getJCas());
        documentJCases.put(document, userCases);
        results = MonitoringPage.computeKappa(Arrays.asList(new User[] { user1, user2 }), adapter,
                "PosValue", userDocs, documentJCases);
        assertEquals(results[0][1], 1.0, 0.0005);
    }

    @Test
    public void testTwoUserDiffArcAnnotation()
        throws Exception
    {
        init();
        double[][] results = new double[2][2];
        TypeAdapter adapter = new ArcAdapter(null, 0, Dependency.class.getName(), "Dependent",
                "Governor", "pos", Token.class.getName());
        Map<User, List<SourceDocument>> userDocs = new HashMap<User, List<SourceDocument>>();
        userDocs.put(user1, Arrays.asList(new SourceDocument[] { document }));
        userDocs.put(user2, Arrays.asList(new SourceDocument[] { document }));
        Map<SourceDocument, Map<User, JCas>> documentJCases = new HashMap<SourceDocument, Map<User, JCas>>();
        Map<User, JCas> userCases = new HashMap<User, JCas>();
        userCases.put(user1, cas1.getJCas());
        userCases.put(user2, cas3.getJCas());
        documentJCases.put(document, userCases);
        results = MonitoringPage.computeKappa(Arrays.asList(new User[] { user1, user2 }), adapter,
                "DependencyType", userDocs, documentJCases);
        assertEquals(results[0][1], 0.82, 0.0005);
    }

    @Test
    public void testTwoUserDiffSpanAnnotation()
        throws Exception
    {
        init();
        double[][] results = new double[2][2];
        AnnotationLayer layer = new AnnotationLayer();
        layer.setId(0);
        layer.setName(POS.class.getName());
        TypeAdapter adapter = new SpanAdapter(layer);
        Map<User, List<SourceDocument>> userDocs = new HashMap<User, List<SourceDocument>>();
        userDocs.put(user1, Arrays.asList(new SourceDocument[] { document }));
        userDocs.put(user2, Arrays.asList(new SourceDocument[] { document }));
        Map<SourceDocument, Map<User, JCas>> documentJCases = new HashMap<SourceDocument, Map<User, JCas>>();
        Map<User, JCas> userCases = new HashMap<User, JCas>();
        userCases.put(user1, cas1.getJCas());
        userCases.put(user2, cas2.getJCas());
        documentJCases.put(document, userCases);
        results = MonitoringPage.computeKappa(Arrays.asList(new User[] { user1, user2 }), adapter,
                "PosValue", userDocs, documentJCases);
        assertEquals(results[0][1], 0.86, 0.0005);
    }

    @Test
    public void testTwoUserDiffArcAndSpanAnnotation()
        throws Exception
    {
        init();
        double[][] results = new double[2][2];
        TypeAdapter adapter = new ArcAdapter(null, 0, Dependency.class.getName(), "Dependent",
                "Governor", "pos", Token.class.getName());
        Map<User, List<SourceDocument>> userDocs = new HashMap<User, List<SourceDocument>>();
        userDocs.put(user1, Arrays.asList(new SourceDocument[] { document }));
        userDocs.put(user2, Arrays.asList(new SourceDocument[] { document }));
        Map<SourceDocument, Map<User, JCas>> documentJCases = new HashMap<SourceDocument, Map<User, JCas>>();
        Map<User, JCas> userCases = new HashMap<User, JCas>();
        userCases.put(user1, cas1.getJCas());
        userCases.put(user2, cas4.getJCas());
        documentJCases.put(document, userCases);
        results = MonitoringPage.computeKappa(Arrays.asList(new User[] { user1, user2 }), adapter,
                "DependencyType", userDocs, documentJCases);
        assertEquals(results[0][1], 0.64, 0.0005);
    }

    @Test
    public void testThreeUserDiffArcAndSpanAnnotation()
        throws Exception
    {
        init();
        double[][] results = new double[2][2];
        TypeAdapter adapter = new ArcAdapter(null, 0, Dependency.class.getName(), "Dependent",
                "Governor", "pos", Token.class.getName());
        Map<User, List<SourceDocument>> userDocs = new HashMap<User, List<SourceDocument>>();
        userDocs.put(user1, Arrays.asList(new SourceDocument[] { document }));
        userDocs.put(user2, Arrays.asList(new SourceDocument[] { document }));
        userDocs.put(user3, Arrays.asList(new SourceDocument[] { document }));
        Map<SourceDocument, Map<User, JCas>> documentJCases = new HashMap<SourceDocument, Map<User, JCas>>();
        Map<User, JCas> userCases = new HashMap<User, JCas>();
        userCases.put(user1, cas1.getJCas());
        userCases.put(user2, cas2.getJCas());
        userCases.put(user3, cas4.getJCas());
        documentJCases.put(document, userCases);
        results = MonitoringPage.computeKappa(Arrays.asList(new User[] { user1, user2, user3 }),
                adapter, "DependencyType", userDocs, documentJCases);
        assertEquals(results[0][1], 0.94, 0.0005); // user1 V user2
        assertEquals(results[0][2], 0.64, 0.0005);// user1 V user3
        assertEquals(results[1][2], 0.58, 0.0005);// user2 V user3
    }
}
