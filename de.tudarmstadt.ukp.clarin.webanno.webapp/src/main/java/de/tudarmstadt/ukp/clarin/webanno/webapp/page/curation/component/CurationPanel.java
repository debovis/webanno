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
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotator;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.AnnotationOption;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.AnnotationSelection;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.BratCuratorUtility;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.CasDiff;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.CurationBuilder;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.CurationContainer;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.CurationSegmentForSourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.CurationUserSegmentForAnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.SentenceContainer;

/**
 * Main Panel for the curation page. It displays a box with the complete text on the left side and a
 * box for a selected sentence on the right side.
 *
 * @author Andreas Straninger
 */
public class CurationPanel
    extends Panel
{
    private static final long serialVersionUID = -5128648754044819314L;

    private final static Log LOG = LogFactory.getLog(CurationPanel.class);

    @SpringBean(name = "jsonConverter")
    private MappingJacksonHttpMessageConverter jsonConverter;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    public final static String CURATION_USER = "CURATION_USER";

    private SentenceContainer sentenceOuterView;
    private BratAnnotator mergeVisualizer;

    /**
     * Map for tracking curated spans. Key contains the address of the span, the value contains the
     * username from which the span has been selected
     */
    private Map<String, Map<Integer, AnnotationSelection>> annotationSelectionByUsernameAndAddress = new HashMap<String, Map<Integer, AnnotationSelection>>();

    private ListView<CurationUserSegmentForAnnotationDocument> sentenceListView;

    private CurationSegmentForSourceDocument curationSegment;

    ListView<CurationSegmentForSourceDocument> textListView;

    /**
     * Class for combining an on click ajax call and a label
     */
    class AjaxLabel
        extends Label
    {

        private static final long serialVersionUID = -4528869530409522295L;
        private AbstractAjaxBehavior click;

        public AjaxLabel(String id, String label, AbstractAjaxBehavior click)
        {
            super(id, label);
            this.click = click;
        }

        @Override
        public void onComponentTag(ComponentTag tag)
        {
            // add onclick handler to the browser
            // if clicked in the browser, the function
            // click.response(AjaxRequestTarget target) is called on the server side
            tag.put("ondblclick", "wicketAjaxGet('" + click.getCallbackUrl() + "')");
            tag.put("onclick", "wicketAjaxGet('" + click.getCallbackUrl() + "')");
        }

    }

    public CurationPanel(String id, final CurationContainer curationContainer)
    {
        super(id);

        // add container for updating ajax
        final WebMarkupContainer textOuterView = new WebMarkupContainer("textOuterView");
        textOuterView.setOutputMarkupId(true);
        add(textOuterView);

        /*
         * final WebMarkupContainer sentenceOuterView = new WebMarkupContainer("sentenceOuterView");
         * sentenceOuterView.setOutputMarkupId(true); add(sentenceOuterView);
         */

        final BratAnnotatorModel bratAnnotatorModel = curationContainer.getBratAnnotatorModel();

        LinkedList<CurationUserSegmentForAnnotationDocument> sentences = new LinkedList<CurationUserSegmentForAnnotationDocument>();
        CurationUserSegmentForAnnotationDocument curationUserSegmentForAnnotationDocument = new CurationUserSegmentForAnnotationDocument();
        if (bratAnnotatorModel != null) {
            curationUserSegmentForAnnotationDocument
                    .setAnnotationSelectionByUsernameAndAddress(annotationSelectionByUsernameAndAddress);
            curationUserSegmentForAnnotationDocument.setBratAnnotatorModel(bratAnnotatorModel);
            sentences.add(curationUserSegmentForAnnotationDocument);
        }
        sentenceOuterView = new SentenceContainer("sentenceOuterView",
                new Model<LinkedList<CurationUserSegmentForAnnotationDocument>>(sentences))
        {
            private static final long serialVersionUID = 2583509126979792202L;

            @Override
            protected void onChange(AjaxRequestTarget aTarget)
            {
                // aTarget.add(mergeVisualizer);
                updateRightSide(aTarget, this, curationContainer, mergeVisualizer);
            }
        };

        sentenceOuterView.setOutputMarkupId(true);
        add(sentenceOuterView);

        mergeVisualizer = new BratAnnotator("mergeView", new Model<BratAnnotatorModel>(
                bratAnnotatorModel))
        {

            private static final long serialVersionUID = 7279648231521710155L;

            @Override
            protected void onChange(AjaxRequestTarget aTarget)
            {
                aTarget.add(sentenceOuterView);
                updateRightSide(aTarget, sentenceOuterView, curationContainer, this);
            }
        };
        // reset sentenceAddress and lastSentenceAddress to the orginal once

        mergeVisualizer.setOutputMarkupId(true);
        add(mergeVisualizer);

        textListView = new ListView<CurationSegmentForSourceDocument>("textListView",
                curationContainer.getCurationSegments())
        {
            @Override
            protected void populateItem(ListItem<CurationSegmentForSourceDocument> item)
            {
                final CurationSegmentForSourceDocument curationSegmentItem = item.getModelObject();

                // ajax call when clicking on a sentence on the left side
                final AbstractDefaultAjaxBehavior click = new AbstractDefaultAjaxBehavior()
                {

                    @Override
                    protected void respond(AjaxRequestTarget target)
                    {
                        curationSegment = curationSegmentItem;
                        updateRightSide(target, sentenceOuterView, curationContainer,
                                mergeVisualizer);
                        List<CurationSegmentForSourceDocument> segments = curationContainer
                                .getCurationSegments();
                        for (CurationSegmentForSourceDocument segment : segments) {
                            segment.setCurrentSentence(curationSegmentItem.getSentenceNumber()
                                    .equals(segment.getSentenceNumber()));
                        }
                        textListView.setModelObject(segments);
                        textOuterView.addOrReplace(textListView);
                        target.add(textOuterView);
                        target.add(sentenceOuterView);
                        // target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
                    }

                };

                // add subcomponents to the component
                item.add(click);
                String colorCode = curationSegmentItem.getSentenceState().getColorCode();
                /*
                 * if (curationSegmentItem.isCurrentSentence()) {
                 * item.add(AttributeModifier.append("style", "border: 4px solid black;")); }
                 */
                if (colorCode != null) {
                    item.add(AttributeModifier.append("style", "background-color: " + colorCode
                            + ";"));
                }

                Label currentSentence = new AjaxLabel("sentence", curationSegmentItem.getText(),
                        click);
                item.add(currentSentence);

                Label sentenceNumber = new AjaxLabel("sentenceNumber", curationSegmentItem
                        .getSentenceNumber().toString(), click);
                item.add(sentenceNumber);
            }

        };
        // add subcomponents to the component
        textListView.setOutputMarkupId(true);
        textOuterView.add(textListView);

    }

    protected void updateRightSide(AjaxRequestTarget target, SentenceContainer parent,
            CurationContainer curationContainer, BratAnnotator mergeVisualizer)
    {
        SourceDocument sourceDocument = curationContainer.getBratAnnotatorModel().getDocument();
        Project project = curationContainer.getBratAnnotatorModel().getProject();
        List<AnnotationDocument> annotationDocuments = repository.listAnnotationDocument(project,
                sourceDocument);
        Map<String, JCas> jCases = new HashMap<String, JCas>();
        JCas mergeJCas = null;
        try {
            mergeJCas = repository.getCurationDocumentContent(sourceDocument);
            // get cases from repository
            BratCuratorUtility.getCases(jCases, annotationDocuments, repository,
                    annotationSelectionByUsernameAndAddress);

            // add mergeJCas separately
            jCases.put(CURATION_USER, mergeJCas);

            // get differing feature structures
            List<Type> entryTypes = CurationBuilder.getEntryTypes(mergeJCas,
                    curationContainer.getBratAnnotatorModel());
            List<AnnotationOption> annotationOptions = null;
            try {
                annotationOptions = CasDiff.doDiff(entryTypes, jCases, curationSegment.getBegin(),
                        curationSegment.getEnd());
            }
            catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // fill lookup variable for annotation selections
            BratCuratorUtility.fillLookupVariables(annotationOptions,
                    annotationSelectionByUsernameAndAddress);

            LinkedList<CurationUserSegmentForAnnotationDocument> sentences = new LinkedList<CurationUserSegmentForAnnotationDocument>();

            BratAnnotatorModel bratAnnotatorModel = BratCuratorUtility.setBratAnnotatorModel(
                    sourceDocument, repository, curationSegment, annotationService);

            BratCuratorUtility.populateCurationSentences(jCases, sentences, bratAnnotatorModel,
                    annotationOptions, annotationSelectionByUsernameAndAddress, jsonConverter);
            // update sentence list on the right side
            parent.setModelObject(sentences);

            bratAnnotatorModel.setMode(Mode.MERGE);
            mergeVisualizer.setModelObject(bratAnnotatorModel);
            mergeVisualizer.reloadContent(target);

            target.add(parent);
        }
        catch (UIMAException e1) {
            error(ExceptionUtils.getRootCause(e1));
        }
        catch (IOException e1) {
            error(e1.getMessage());
        }
        catch (ClassNotFoundException e1) {
            error(e1.getMessage());
        }

    }

}
