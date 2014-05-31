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
package de.tudarmstadt.ukp.clarin.webanno.tsv;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import de.tudarmstadt.ukp.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/**
 * Reads a specific TSV File (9 TAB separated) annotation and change it to CAS object. Example of
 * Input Files: <br>
 * 1 Heutzutage heutzutage ADV _ _ 2 ADV _ _ <br>
 * Columns are separated by a TAB character and sentences are separated by a blank new line see the
 * {@link WebannoTsvReader#setAnnotations(InputStream, String, StringBuilder, Map, Map, Map, Map, Map, Map, Map, List)}
 *
 * @author Seid Muhie Yimam
 *
 */
public class WebannoTsvReader
    extends JCasResourceCollectionReader_ImplBase
{

    private String fileName;

    public void convertToCas(JCas aJCas, InputStream aIs, String aEncoding)
        throws IOException

    {
        StringBuilder text = new StringBuilder();
        Map<Integer, String> tokens = new HashMap<Integer, String>();
        Map<Integer, String> pos = new HashMap<Integer, String>();
        Map<Integer, String> lemma = new HashMap<Integer, String>();
        Map<Integer, String> namedEntity = new HashMap<Integer, String>();
        Map<Integer, String> dependencyFunction = new HashMap<Integer, String>();
        Map<Integer, Integer> dependencyDependent = new HashMap<Integer, Integer>();

        List<Integer> firstTokenInSentence = new ArrayList<Integer>();

        DocumentMetaData documentMetadata = DocumentMetaData.get(aJCas);
        fileName = documentMetadata.getDocumentTitle();
        setAnnotations(aJCas, aIs, aEncoding, text, tokens, pos, lemma, namedEntity,
                dependencyFunction, dependencyDependent, firstTokenInSentence);

        aJCas.setDocumentText(text.toString());

        /*
         * Map<String, Token> tokensStored = new HashMap<String, Token>();
         *
         * createToken(aJCas, text, tokens, pos, lemma, tokensStored);
         *
         * createNamedEntity(namedEntity, aJCas, tokens, tokensStored);
         *
         * createDependency(aJCas, tokens, dependencyFunction, dependencyDependent, tokensStored);
         *
         * createSentence(aJCas, firstTokenInSentence, tokensStored);
         */
    }

    /**
     * Create {@link Token} in the {@link CAS}. If the lemma and pos columns are not empty it will
     * create {@link Lemma} and {@link POS} annotations
     */
    private void createToken(JCas aJCas, StringBuilder text, Map<Integer, String> tokens,
            Map<Integer, String> pos, Map<Integer, String> lemma, Map<String, Token> tokensStored)
    {
        int tokenBeginPosition = 0;
        int tokenEndPosition = 0;

        for (int i = 1; i <= tokens.size(); i++) {
            tokenBeginPosition = text.indexOf(tokens.get(i), tokenBeginPosition);
            Token outToken = new Token(aJCas, tokenBeginPosition, text.indexOf(tokens.get(i),
                    tokenBeginPosition) + tokens.get(i).length());
            tokenEndPosition = text.indexOf(tokens.get(i), tokenBeginPosition)
                    + tokens.get(i).length();
            tokenBeginPosition = tokenEndPosition;
            outToken.addToIndexes();

            // Add pos to CAS if exist
            if (!pos.get(i).equals("_")) {
                POS outPos = new POS(aJCas, outToken.getBegin(), outToken.getEnd());
                outPos.setPosValue(pos.get(i));
                outPos.addToIndexes();
                outToken.setPos(outPos);
            }

            // Add lemma if exist
            if (!lemma.get(i).equals("_")) {
                Lemma outLemma = new Lemma(aJCas, outToken.getBegin(), outToken.getEnd());
                outLemma.setValue(lemma.get(i));
                outLemma.addToIndexes();
                outToken.setLemma(outLemma);
            }
            tokensStored.put("t_" + i, outToken);
        }
    }

    /**
     * add dependency parsing to CAS
     */
    private void createDependency(JCas aJCas, Map<Integer, String> tokens,
            Map<Integer, String> dependencyFunction, Map<Integer, Integer> dependencyDependent,
            Map<String, Token> tokensStored)
    {
        for (int i = 1; i <= tokens.size(); i++) {
            if (dependencyFunction.get(i) != null) {
                Dependency outDependency = new Dependency(aJCas);
                outDependency.setDependencyType(dependencyFunction.get(i));

                // if span A has (start,end)= (20, 26) and B has (start,end)= (30, 36)
                // arc drawn from A to B, dependency will have (start, end) = (20, 36)
                // arc drawn from B to A, still dependency will have (start, end) = (20, 36)
                int begin = 0, end = 0;
                // if not ROOT
                if (dependencyDependent.get(i) != 0) {
                    begin = tokensStored.get("t_" + i).getBegin() > tokensStored.get(
                            "t_" + dependencyDependent.get(i)).getBegin() ? tokensStored.get(
                            "t_" + dependencyDependent.get(i)).getBegin() : tokensStored.get(
                            "t_" + i).getBegin();
                    end = tokensStored.get("t_" + i).getEnd() < tokensStored.get(
                            "t_" + dependencyDependent.get(i)).getEnd() ? tokensStored.get(
                            "t_" + dependencyDependent.get(i)).getEnd() : tokensStored
                            .get("t_" + i).getEnd();
                }
                else {
                    begin = tokensStored.get("t_" + i).getBegin();
                    end = tokensStored.get("t_" + i).getEnd();
                }

                outDependency.setBegin(begin);
                outDependency.setEnd(end);
                outDependency.setDependent(tokensStored.get("t_" + i));
                if (dependencyDependent.get(i) == 0) {
                    outDependency.setGovernor(tokensStored.get("t_" + i));
                }
                else {
                    outDependency.setGovernor(tokensStored.get("t_" + dependencyDependent.get(i)));
                }
                outDependency.addToIndexes();
            }
        }
    }

    /**
     * Add sentence layer to CAS
     */
    private void createSentence(JCas aJCas, List<Integer> firstTokenInSentence,
            Map<String, Token> tokensStored)
    {
        for (int i = 0; i < firstTokenInSentence.size(); i++) {
            Sentence outSentence = new Sentence(aJCas);
            // Only last sentence, and no the only sentence in the document (i!=0)
            if (i == firstTokenInSentence.size() - 1 && i != 0) {
                outSentence.setBegin(tokensStored.get("t_" + firstTokenInSentence.get(i)).getEnd());
                outSentence.setEnd(tokensStored.get("t_" + (tokensStored.size())).getEnd());
                outSentence.addToIndexes();
                break;
            }
            if (i == firstTokenInSentence.size() - 1 && i == 0) {
                outSentence.setBegin(tokensStored.get("t_" + firstTokenInSentence.get(i))
                        .getBegin());
                outSentence.setEnd(tokensStored.get("t_" + (tokensStored.size())).getEnd());
                outSentence.addToIndexes();
            }
            else if (i == 0) {
                outSentence.setBegin(tokensStored.get("t_" + firstTokenInSentence.get(i))
                        .getBegin());
                outSentence.setEnd(tokensStored.get("t_" + firstTokenInSentence.get(i + 1))
                        .getEnd());
                outSentence.addToIndexes();
            }
            else {
                outSentence
                        .setBegin(tokensStored.get("t_" + firstTokenInSentence.get(i)).getEnd() + 1);
                outSentence.setEnd(tokensStored.get("t_" + firstTokenInSentence.get(i + 1))
                        .getEnd());
                outSentence.addToIndexes();
            }
        }
    }

    /**
     * Iterate through all lines and get available annotations<br>
     * First column is sentence number and a blank new line marks end of a sentence<br>
     * The Second column is the token <br>
     * The third column is the lemma annotation <br>
     * The fourth column is the POS annotation <br>
     * The fifth column is used for Named Entity annotations (Multiple annotations separeted by |
     * character) <br>
     * The sixth column is the origin token number of dependency parsing <br>
     * The seventh column is the function/type of the dependency parsing <br>
     * eighth and ninth columns are undefined currently
     */
    private void setAnnotations(JCas aJcas, InputStream aIs, String aEncoding, StringBuilder text,
            Map<Integer, String> tokens, Map<Integer, String> pos, Map<Integer, String> lemma,
            Map<Integer, String> namedEntity, Map<Integer, String> dependencyFunction,
            Map<Integer, Integer> dependencyDependent, List<Integer> firstTokenInSentence)
        throws IOException
    {

        // getting header information
        LineIterator lineIterator = IOUtils.lineIterator(aIs, aEncoding);
        int columns = 1;// token number + token columns (minimum required)
        int tokenStart = 0, sentenceStart = 0;
        Map<Type, Set<Feature>> layers = new LinkedHashMap<Type, Set<Feature>>();
        // start of an annotation for a layer
        Map<Type, Integer> annotationBegin = new HashMap<Type, Integer>();
        // an annotation for every feature in a layer
        Map<Type, Map<Integer, AnnotationFS>> annotations = new LinkedHashMap<Type, Map<Integer, AnnotationFS>>();
     //   Map<Type, String> beginEndAnnotation = new HashMap<Type, String>();
        Map<Type, Map<Integer, String>> beginEndAnno = new LinkedHashMap<Type, Map<Integer, String>>();

        while (lineIterator.hasNext()) {
            String line = lineIterator.next().trim();
            if (line.trim().equals("")) {
            	text.replace(tokenStart-1, tokenStart, "");
            	tokenStart = tokenStart-1;
                Sentence sentence = new Sentence(aJcas, sentenceStart, tokenStart);
                sentence.addToIndexes();
                tokenStart++;
                sentenceStart = tokenStart;
                text.append("\n");
                continue;
            }
            if (line.startsWith("#text=")) {
                continue;
            }
            if (line.startsWith("#id=")) {
                continue;// it is a comment line
            }

            if (line.startsWith("#")) {
                StringTokenizer headerTk = new StringTokenizer(line, "#");
                while (headerTk.hasMoreTokens()) {
                    String layerNames = headerTk.nextToken().trim();
                    StringTokenizer layerTk = new StringTokenizer(layerNames, "|");

                    Set<Feature> features = new LinkedHashSet<Feature>();
                    String layerName = layerTk.nextToken().trim();
                    Type layer = CasUtil.getType(aJcas.getCas(), layerName);
                    while (layerTk.hasMoreTokens()) {
                        Feature feature = layer.getFeatureByBaseName(layerTk.nextToken().trim());
                        features.add(feature);
                        columns++;
                    }

                    layers.put(layer, features);
                }
                continue;
            }

            int count = StringUtils.countMatches(line, "\t");

            if (columns != count) {
                throw new IOException(fileName + " This is not a valid TSV File. check this line: "
                        + line);
            }

            // adding tokens and sentence
            StringTokenizer lineTk = new StringTokenizer(line, "\t");
            String tokenNumberColumn = lineTk.nextToken();
            int tokenNumber = Integer.parseInt(tokenNumberColumn.split("-")[1]);
            String tokenColumn = lineTk.nextToken();
            Token token = new Token(aJcas, tokenStart, tokenStart + tokenColumn.length());
            token.addToIndexes();

            // adding the annotations

            for (Type layer : layers.keySet()) {
                boolean existsAnnotation = false;
                for (Feature feature : layers.get(layer)) {
                    int index = 1;
                    String multipleAnnotations = lineTk.nextToken();
                    for (String annotation : multipleAnnotations.split("\\|")) {
                        // for annotations such as B_LOC|O|I_PER and the like
                        
                          if (annotation.equals("B-_") || annotation.equals("I-_")) { index++; }
                          else
                         if (annotation.startsWith("B-")) {
                            existsAnnotation = true;
                            Map<Integer, AnnotationFS> indexedAnnos = annotations.get(layer);
                            Map<Integer, String> indexedBeginEndAnnos = beginEndAnno.get(layer);
                            AnnotationFS newAnnotation;

                            if (indexedAnnos == null) {
                                newAnnotation = aJcas.getCas().createAnnotation(layer, tokenStart,
                                        tokenStart + tokenColumn.length());
                                indexedAnnos = new LinkedHashMap<Integer, AnnotationFS>();
                                indexedBeginEndAnnos = new LinkedHashMap<Integer, String>();
                            }
                            else if (indexedAnnos.get(index) == null) {
                                newAnnotation = aJcas.getCas().createAnnotation(layer, tokenStart,
                                        tokenStart + tokenColumn.length());
                            }
                            else if(indexedBeginEndAnnos.get(index).equals("I-")){
                            	 newAnnotation = aJcas.getCas().createAnnotation(layer, tokenStart,
                                         tokenStart + tokenColumn.length());
                            }
                       /*     // for consecutive annotations such as B-anno I-anno B-anno
                            else if (beginEndAnnotation.get(layer) != null
                                    && beginEndAnnotation.get(layer).equals("I-")) {
                                newAnnotation = aJcas.getCas().createAnnotation(layer, tokenStart,
                                        tokenStart + tokenColumn.length());
                                indexedAnnos = new LinkedHashMap<Integer, AnnotationFS>();
                                beginEndAnnotation.put(layer, null);
                            }
                            // consequetive Token Annotation such as B-LOC B-LOC
                            else if (beginEndAnnotation.get(layer) != null
                                    && beginEndAnnotation.get(layer).equals("B-")) {
                                newAnnotation = aJcas.getCas().createAnnotation(layer, tokenStart,
                                        tokenStart + tokenColumn.length());
                                indexedAnnos = new LinkedHashMap<Integer, AnnotationFS>();
                                beginEndAnnotation.put(layer, null);
                            }*/
                            else {
                                newAnnotation = indexedAnnos.get(index);
                                
                            }

                            // remove prefixes such as B-/I- before creating the annotation
                            newAnnotation.setFeatureValueFromString(feature,
                                    (annotation.substring(2)));
                            aJcas.addFsToIndexes(newAnnotation);
                            indexedAnnos.put(index, newAnnotation);
                            indexedBeginEndAnnos.put(index, "B-");
                            annotations.put(layer, indexedAnnos);
                            
                            beginEndAnno.put(layer, indexedBeginEndAnnos);
                            index++;
                        }
                        else if (annotation.startsWith("I-")) {
                           // beginEndAnnotation.put(layer, "I-");
  
                            Map<Integer, String> indexedBeginEndAnnos = beginEndAnno.get(layer);
                            indexedBeginEndAnnos.put(index, "I-");
                            beginEndAnno.put(layer, indexedBeginEndAnnos);
                            
                            Map<Integer, AnnotationFS> indexedAnnos = annotations.get(layer);
                            AnnotationFS newAnnotation = indexedAnnos.get(index);
                            ((Annotation) newAnnotation).setEnd(tokenStart + tokenColumn.length());
                            aJcas.addFsToIndexes(newAnnotation);
                            index++;
                        }
                        else {
                            annotations.put(layer, null);
                            index++;
                        }
                    }
                }
      /*          if (existsAnnotation
                        && (beginEndAnnotation.get(layer) == null || !beginEndAnnotation.get(layer)
                                .equals("I-"))) {
                    beginEndAnnotation.put(layer, "B-");
                }*/
            }
            
            tokenStart = tokenStart + tokenColumn.length() + 1;
            text.append(tokenColumn + " ");
        }

        /*
         * int tokenNumber = 0; boolean first = true; int base = 0;
         *
         * lineIterator = IOUtils.lineIterator(aIs, aEncoding); boolean textFound = false;
         * StringBuffer tmpText = new StringBuffer(); while (lineIterator.hasNext()) { String line =
         * lineIterator.next().trim(); if (line.startsWith("#text=")) {
         * text.append(line.substring(6) + "\n"); textFound = true; continue; } if
         * (line.startsWith("#")) { continue;// it is a comment line } int count =
         * StringUtils.countMatches(line, "\t"); if (line.isEmpty()) { continue; } if (count != 9)
         * {// not a proper TSV file getUimaContext().getLogger().log(Level.INFO,
         * "This is not a valid TSV File"); throw new IOException(fileName +
         * " This is not a valid TSV File"); } StringTokenizer lineTk = new StringTokenizer(line,
         * "\t");
         *
         * if (first) { tokenNumber = Integer.parseInt(line.substring(0, line.indexOf("\t")));
         * firstTokenInSentence.add(tokenNumber); first = false; } else { int lineNumber =
         * Integer.parseInt(line.substring(0, line.indexOf("\t"))); if (lineNumber == 1) { base =
         * tokenNumber; firstTokenInSentence.add(base); } tokenNumber = base +
         * Integer.parseInt(line.substring(0, line.indexOf("\t"))); }
         *
         * while (lineTk.hasMoreElements()) { lineTk.nextToken(); String token = lineTk.nextToken();
         *
         * // for backward compatibility tmpText.append(token + " ");
         *
         * tokens.put(tokenNumber, token); lemma.put(tokenNumber, lineTk.nextToken());
         * pos.put(tokenNumber, lineTk.nextToken()); String ne = lineTk.nextToken();
         * lineTk.nextToken();// make it compatible with prev WebAnno TSV reader
         * namedEntity.put(tokenNumber, (ne.equals("_") || ne.equals("-")) ? "O" : ne); String
         * dependentValue = lineTk.nextToken(); if (NumberUtils.isDigits(dependentValue)) { int
         * dependent = Integer.parseInt(dependentValue); dependencyDependent.put(tokenNumber,
         * dependent == 0 ? 0 : base + dependent); dependencyFunction.put(tokenNumber,
         * lineTk.nextToken()); } else { lineTk.nextToken(); } lineTk.nextToken();
         * lineTk.nextToken(); } } if (!textFound) { text.append(tmpText); }
         */
    }

    private void createAnnotation(JCas aJcas, int tokenStart, Map<Type, Integer> annotationBegin,
            Map<Feature, String> annotations, String tokenColumn, Type layer, Feature feature,
            String annotation)
    {
        // check if annotation (for other features) exists
        for (AnnotationFS fs : CasUtil.selectCovered(aJcas.getCas(), layer,
                annotationBegin.get(layer), tokenStart)) {
            if (fs.getBegin() == annotationBegin.get(layer) && fs.getEnd() == tokenStart) {
                // annotation created for another feature
                if (fs.getFeatureValueAsString(feature) == null) {
                    fs.setFeatureValueFromString(feature, annotations.get(feature).substring(2));
                    aJcas.addFsToIndexes(fs);
                    return;
                }
                // otherwise, stacking is allowed and create new annotation
                else if (fs.getFeatureValueAsString(feature).equals(
                        annotations.get(feature).substring(2))) {
                    break;

                }
            }
        }

        AnnotationFS newAnnotation = aJcas.getCas().createAnnotation(layer,
                annotationBegin.get(layer), tokenStart);
        // remove prefixes such as B-/I- before creating the annotation
        newAnnotation.setFeatureValueFromString(feature, annotations.get(feature).substring(2));
        aJcas.addFsToIndexes(newAnnotation);
        // store new annotations now
        annotations.put(feature, annotation);
    }

    public static final String PARAM_ENCODING = ComponentParameters.PARAM_SOURCE_ENCODING;
    @ConfigurationParameter(name = PARAM_ENCODING, mandatory = true, defaultValue = "UTF-8")
    private String encoding;

    @Override
    public void getNext(JCas aJCas)
        throws IOException, CollectionException
    {
        Resource res = nextFile();
        initCas(aJCas, res);
        InputStream is = null;
        try {
            is = res.getInputStream();
            convertToCas(aJCas, is, encoding);
        }
        finally {
            closeQuietly(is);
        }

    }

    /**
     * Creates Named Entities from CoNLL BIO format to CAS format
     */
    private void createNamedEntity(Map<Integer, String> aNamedEntityMap, JCas aJCas,
            Map<Integer, String> aTokensMap, Map<String, Token> aJcasTokens)
    {

        Map<Integer, NamedEntity> indexedNeAnnos = new LinkedHashMap<Integer, NamedEntity>();

        for (int i = 1; i <= aTokensMap.size(); i++) {
            if (aNamedEntityMap.get(i).equals("O")) {
                continue;
            }
            int index = 1;// to maintain multiple span ne annotation in the same index
            for (String ne : aNamedEntityMap.get(i).split("\\|")) {

                if (ne.equals("O")) {// for annotations such as B_LOC|O|I_PER and the like
                    index++;
                }
                else if (ne.startsWith("B_") || ne.startsWith("B-")) {
                    NamedEntity outNamedEntity = new NamedEntity(aJCas, aJcasTokens.get("t_" + i)
                            .getBegin(), aJcasTokens.get("t_" + i).getEnd());
                    outNamedEntity.setValue(ne.substring(2));
                    outNamedEntity.addToIndexes();
                    indexedNeAnnos.put(index, outNamedEntity);
                    index++;
                }
                else if (ne.startsWith("I_") || ne.startsWith("I-")) {
                    NamedEntity outNamedEntity = indexedNeAnnos.get(index);
                    outNamedEntity.setEnd(aJcasTokens.get("t_" + i).getEnd());
                    outNamedEntity.addToIndexes();
                    index++;
                }
                else {// NE is not in IOB format. store one NE per token. No way to detect multiple
                      // token NE
                    NamedEntity outNamedEntity = new NamedEntity(aJCas, aJcasTokens.get("t_" + i)
                            .getBegin(), aJcasTokens.get("t_" + i).getEnd());
                    outNamedEntity.setValue(ne);
                    outNamedEntity.addToIndexes();
                    indexedNeAnnos.put(index, outNamedEntity);
                    index++;
                }
            }
        }
    }
}