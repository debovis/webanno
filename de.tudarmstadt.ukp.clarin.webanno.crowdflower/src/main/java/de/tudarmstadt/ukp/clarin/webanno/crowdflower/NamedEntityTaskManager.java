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
package de.tudarmstadt.ukp.clarin.webanno.crowdflower;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;
import static org.uimafit.util.JCasUtil.select;
import static org.uimafit.util.JCasUtil.selectCovered;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.jcas.JCas;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;

import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class NamedEntityTaskManager implements Serializable
{

    private static final long serialVersionUID = -166689748276508297L;

    private CrowdClient crowdclient;

    private static final String noGoldNER1Reason = "Dieser Textabschnitt beinhaltet keine Named Entities (in solchen F&auml;llen m&uuml;ssen sie auf den daf&uuml;r vorgesehen Button klicken. Es reicht nicht aus nichts zu makieren). Sie k&ouml;nnen diese Meinung anfechten und uns erkl&auml;ren warum Sie meinen das dies falsch ist.\n";
    private static final String goldNER1ReasonHints = "\n <br/> Tipps: Wenn eine Named Entity l&auml;nger als ein Wort lang ist, m&uuml;ssen sie beim ersten Wort anfangen zu makieren und beim letzten Wort loslassen. Falsch ist: Klicken Sie stattdessen auf die W&ouml;rter einzeln, erzeugt dies mehrere einzelne Makierungen! Sie k&ouml;nnen auch nur bis z.B. zu der H&auml;lfte eines Wortes makieren, dies erfasst trotzdem das ganze Wort. \n";


    public NamedEntityTaskManager()
    {
        crowdclient = new CrowdClient();
    }

    /**
     * Generates a new job on crowdflower.com based on the supplied template string.
     * The new job won't have any data items
     *
     * @param template
     * @return
     * @throws JsonProcessingException
     * @throws IOException
     */
    public CrowdJob createJob(String template) throws JsonProcessingException, IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonTemplate = mapper.readTree(template);
        CrowdJob job = new CrowdJob(jsonTemplate);
        return crowdclient.createNewJob(job);
    }

    public static String concatWithSeperator(Collection<String> words, String seperator) {
        StringBuilder wordList = new StringBuilder();
        for (String word : words) {
            wordList.append(new String(word) + seperator);
        }
        return new String(wordList.deleteCharAt(wordList.length() - 1));
    }

 /**
  * Generate data for NER task1. This is quite specific code, but a more general version could one day extend this.
  * @param documentsJCas List of Cas containing either the documents to be annotated or gold documents
  * @param goldOffset This is an offset to the token number that is send to Crowdflower, so that (i - goldOffset) = real token offset in webanno.
  *                     This is needed so that continuous token numbers can be send to Crowdflower
  * @param generateGold
  * @return
 * @throws Exception
  */
    public Vector<NamedEntityTask1Data> generateTask1Data(List<JCas>documentsJCas, int goldOffset, boolean generateGold, int limit) throws Exception
    {
        Log LOG = LogFactory.getLog(getClass());
        Vector<NamedEntityTask1Data> data = new Vector<NamedEntityTask1Data>();
        int i=goldOffset;
        StringBuilder textBuilder = new StringBuilder();
        int docNo = 1;

        //DecimalFormat percentFormat = new DecimalFormat("0.00");

        jcasloop:
        for (JCas documentJCas : documentsJCas)
        {
            int offset = i;
            int documentSize = documentJCas.size();

            int sentenceNo = 0;
            LOG.info("Generating data for document: " + docNo + "/" + documentsJCas.size());
            LOG.info("Document size: " + documentSize);
            for (Sentence sentence : select(documentJCas, Sentence.class)) {

                //if global limit of sentences reached, abort whole iteration
                if(limit != -1 && sentenceNo >= limit) {
                    break jcasloop;
                }

                textBuilder.setLength(0);

                //debug: output progress every 200 sentences
                /*if(sentenceNo % 200 == 0) {
                    LOG.info(percentFormat.format(((float)sentence.getBegin() / (float)documentSize)*1000.0)+"%");
                }*/

                //Maps our own token offsets (needed by JS in the crowdflower task) to Jcas offsets
                HashMap<Integer,Integer> charOffsetStartMapping = new HashMap<Integer,Integer>();
                HashMap<Integer,Integer> charOffsetEndMapping = new HashMap<Integer,Integer>();

                for (Token token : selectCovered(Token.class, sentence)) {

                    //check that token offsets match to (i - goldOffset)
                    String tokenString = new String(token.getCoveredText());
                    textBuilder.append("<span id=\"token=");
                    textBuilder.append(String.valueOf(i));
                    textBuilder.append("\">");
                    textBuilder.append(escapeHtml(tokenString));
                    textBuilder.append(" </span>");

                    charOffsetStartMapping.put(token.getBegin(), i);
                    charOffsetEndMapping.put(token.getEnd(), i);

                    i++;
                }

                String text = new String(textBuilder);

                //clear string builder
                textBuilder.setLength(0);

                //System.out.println(text);
                NamedEntityTask1Data task1Data = new NamedEntityTask1Data(text);
                task1Data.setOffset(offset);

                if (generateGold)
                {
                    ArrayList<String> goldElems = new ArrayList<String>();
                    ArrayList<String> goldTokens = new ArrayList<String>();

                    for (NamedEntity namedEntity : selectCovered(NamedEntity.class, sentence))
                    {
                        List<Token> tokens = selectCovered(documentJCas, Token.class, namedEntity.getBegin(),
                                namedEntity.getEnd());

                        List<String> strTokens = new ArrayList<String>();

                        //transform List<Tokens> to List<Strings>
                        for (Token t : tokens )
                        {
                            strTokens.add(new String(t.getCoveredText()));
                        }

                        String strToken = concatWithSeperator(strTokens," ");

                        goldElems.add(buildTask1TokenJSON(textBuilder, namedEntity,charOffsetStartMapping,charOffsetEndMapping));
                        goldTokens.add(strToken);
                    }

                    //Standard case: no gold elements
                    String strGold = "[]";
                    String strGoldReason = noGoldNER1Reason;

                    //Case where we have gold elements and want to give feedback to the crowd user
                    if(goldElems.size() > 0)
                    {
                        strGold = buildTask1GoldElem(textBuilder, goldElems);
                        strGoldReason = buildTask1GoldElemReason(textBuilder, goldTokens);
                    }

                    task1Data.setMarkertext_gold(strGold);
                    task1Data.setMarkertext_gold_reason(strGoldReason);

                    // Marker flag for crowdflower that this data is gold data.
                    // Still need to click on "convert uploaded gold" manually in the interface.
                    task1Data.set_golden("TRUE");
                }

                data.add(task1Data);
                sentenceNo++;
            }
            docNo++;
        }

        return data;
    }

    /**
     * Helper method for generateTask1Data, builds a string that explains to the user which named entities he had to select
     *
     * @param textBuilder
     * @param goldTokens
     * @return
     */
    private String buildTask1GoldElemReason(StringBuilder textBuilder, ArrayList<String> goldTokens)
    {
        String strGoldReason;
        textBuilder.setLength(0);
        textBuilder.append("Der Text beinhaltet ");
        textBuilder.append(goldTokens.size());
        textBuilder.append(" Named Entiti(es): ");
        textBuilder.append(escapeHtml(concatWithSeperator(goldTokens,", ")));
        textBuilder.append(goldNER1ReasonHints);

        strGoldReason = new String(textBuilder);
        return strGoldReason;
    }

    /**
     * Helper method for generateTask1Data, concatenates all JSON formatted
     * tokens (sorted by position) which is then the gold solution and contains
     * markers to all NE in the text fragment.
     *
     * This same format is produced by the JS in the crowdflower.com task1.
     *
     * @param textBuilder
     * @param goldElems
     * @return
     */
    private String buildTask1GoldElem(StringBuilder textBuilder, ArrayList<String> goldElems)
    {
        String strGold;
        textBuilder.setLength(0);
        //strGold = "["+concatWithSeperator(goldElems,",")+"]";
        textBuilder.append("[");
        textBuilder.append(concatWithSeperator(goldElems,","));
        textBuilder.append("]");
        strGold = new String(textBuilder);
        return strGold;
    }

    /**
     * Helper method for generateTask1Data, builds a single JSON formatted elemented describing
     * start and end position of a named entity.
     * @param textBuilder
     * @param namedEntity
     * @return
     * @throws Exception
     */
    private String buildTask1TokenJSON(StringBuilder textBuilder, NamedEntity namedEntity, HashMap<Integer,Integer> charOffsetStartMapping, HashMap<Integer,Integer> charOffsetEndMapping) throws Exception
    {
        //JSON named enitity marker for the gold data. The JSON here gets enclosed into the data JSON that is uploaded
        //"{\"s\":"+begin+",\"e\":"+end+"}"
        if(!charOffsetStartMapping.containsKey(namedEntity.getBegin())
           || !charOffsetEndMapping.containsKey(namedEntity.getEnd()))
        {
            throw new Exception("Data generation error: char offset to token mapping is inconsistent. Contact developpers!");
        }

        int start = charOffsetStartMapping.get(namedEntity.getBegin());
        int end = charOffsetEndMapping.get(namedEntity.getEnd());

        textBuilder.setLength(0);
        textBuilder.append("{\"s\":");
        textBuilder.append(start);
        textBuilder.append(",\"e\":");
        textBuilder.append(end);
        textBuilder.append("}");

        return new String(textBuilder);
    }

    /**
     * Set apiKey to the underlying Crowdclient which manages the crowdflower.com REST-protocol
     * @param key
     */
    public void setAPIKey(String key)
    {
        Log LOG = LogFactory.getLog(getClass());
        LOG.info("Using apiKey:" + key);
        crowdclient.setApiKey(key);
    }

    public String uploadNewNERTask2(String template, List<JCas>documentsJCas , List<JCas>goldsJCas, String task1Id)
            throws JsonProcessingException, IOException, Exception
    {
        System.out.println();
        return "";
    }

    public String uploadNewNERTask1(String template, List<JCas>documentsJCas , List<JCas>goldsJCas)
            throws JsonProcessingException, IOException, Exception
            {
                return uploadNewNERTask1(template, documentsJCas , goldsJCas, -1 , -1);
            }

    /**
     * Upload a new NER task1 (german) to crowdflower.com. This method is called from the crowd page and is the starting point for a new task1 upload.
     * @param template
     * @param documentsJCas
     * @param goldsJCas
     * @return
     * @throws JsonProcessingException
     * @throws IOException
     * @throws Exception
     */

    public String uploadNewNERTask1(String template, List<JCas>documentsJCas , List<JCas>goldsJCas, int useSents , int useGoldSents)
            throws JsonProcessingException, IOException, Exception
    {
        Log LOG = LogFactory.getLog(getClass());
        LOG.info("Creating new Job for Ner task 1.");
        CrowdJob job = createJob(template);
        LOG.info("Done, new job id is: "+job.getId()+". Now generating data for NER task 1");

        //by default, allow only German speaking countries
        //would be better to make this configurable

        Vector<String> includedCountries = new Vector<String>();
        includedCountries.add("DE");
        includedCountries.add("AT");
        includedCountries.add("CH");
        job.setIncludedCountries(includedCountries);

        crowdclient.updateAllowedCountries(job);

        int goldOffset = 0;

        Vector<NamedEntityTask1Data> goldData = new Vector<NamedEntityTask1Data>();

        //if we have gold data, than generate data for it
        if(goldsJCas != null && goldsJCas.size() > 0)
        {
            LOG.info("Gold data available, generating gold data first.");
            goldData = generateTask1Data(goldsJCas,0,true, useGoldSents);
            goldOffset = goldData.size();
        }

        LOG.info("Generate normal task data.");
        Vector<NamedEntityTask1Data> data = generateTask1Data(documentsJCas,goldOffset,false,useSents);

        Vector<NamedEntityTask1Data> mergedData = new Vector<NamedEntityTask1Data>();

        if(goldsJCas != null && goldsJCas.size() > 0)
        {

            mergedData.addAll(goldData);
        }

        mergedData.addAll(data);

        LOG.info("Job data prepared, starting upload.");
        LOG.info("Uploading data to job #" + job.getId());
        crowdclient.uploadData(job,mergedData);
        LOG.info("Done, finished uploading data to #" + job.getId());
        return job.getId();
    }

    /**
     * Gets called from Crowdflower page to determine URL for a given job
     * @param jobID
     * @return
     */

   public String getURLforID(String jobID)
    {
        return "https://crowdflower.com/jobs/"+jobID+"/";
    }

   public String getStatusString(String jobID1, String jobID2)
   {
       //first case: no job ids
       if((jobID1 == null || jobID1.equals("")) && (jobID2 == null || jobID2.equals("")))
       {
           return "No jobs uploaded.";
       }
       //second case, got first job id
       else if(!(jobID1 == null || jobID1.equals("")) && (jobID2 == null || jobID2.equals("")))
       {
           JsonNode status = crowdclient.getStatus(jobID1);

           int uploadedUnits = -1;
           boolean finsished = false;

           if(status.has("count") && status.has("done"))
           {
               uploadedUnits = status.get("count").getIntValue();
               finsished = status.get("done").getBooleanValue();
           }
           else
           {
               return "Error retrieving status";
           }

           return "Job1 has "+uploadedUnits+" uploaded units and is "+
           (finsished ? " finished. You can continue with task 2." :
               " not yet finished. Check the link for more information on crowdflower.com. You have to finish task1 before doing task2.");
       }else
       {
           return "Todo: not yet implemented";
       }
   }

   /**
    * stub
    * @param template
    * @param jobID1
    * @param documentsJCas
    * @param goldsJCas
    * @return
    * @throws JsonProcessingException
    * @throws IOException
    * @throws Exception
    */
   public String uploadNewNERTask2(String template, String jobID1, List<JCas>documentsJCas , List<JCas>goldsJCas)
           throws JsonProcessingException, IOException, Exception
   {
       Log LOG = LogFactory.getLog(getClass());

       //retrieve job id 1 data
       CrowdJob job1 = crowdclient.retrieveJob(jobID1);
       JsonNode judgments = crowdclient.retrieveJudgments(job1);

       System.out.println(judgments);

       /*
       LOG.info("Creating new Job for Ner task 2.");
       CrowdJob job = createJob(template);
       LOG.info("Done, new job id is: "+job.getId()+". Now generating data for NER task 2");*/



       return "";
   }

}