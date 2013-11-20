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
package de.tudarmstadt.ukp.clarin.webanno.brat.annotation;

import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeUtil.getAdapter;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;

import com.googlecode.wicket.jquery.ui.kendo.combobox.ComboBox;
import com.googlecode.wicket.jquery.ui.kendo.combobox.ComboBoxRenderer;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.AnnotationTypeConstant;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasController;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationType;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

/**
 * A page that is used to display an annotation modal dialog for span annotation
 *
 * @author Seid Muhie Yimam
 *
 */
public class SpanAnnotationModalWindowPage
    extends WebPage
{
    private static final long serialVersionUID = -2102136855109258306L;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    @SpringBean(name = "jsonConverter")
    private MappingJacksonHttpMessageConverter jsonConverter;

    ComboBox<Tag> tags;
    boolean isModify = false;
    TagSet selectedtTagSet;

    Model<TagSet> tagSetsModel;
    Model<String> tagsModel;
    private AnnotationDialogForm annotationDialogForm;
    private BratAnnotatorModel bratAnnotatorModel;
    private int beginOffset;
    private int endOffset;
    private String selectedText = null;
    int selectedSpanId = -1;
    String selectedSpanType;

    private class AnnotationDialogForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -4104665452144589457L;

        public AnnotationDialogForm(String id, final ModalWindow aModalWindow)
        {
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));

            final FeedbackPanel feedbackPanel = new FeedbackPanel("feedbackPanel");
            add(feedbackPanel);
            feedbackPanel.setOutputMarkupId(true);
            feedbackPanel.add(new AttributeModifier("class", "info"));
            feedbackPanel.add(new AttributeModifier("class", "error"));

            List<TagSet> spanLayers = new ArrayList<TagSet>();

            for (TagSet tagset : bratAnnotatorModel.getAnnotationLayers()) {
                if (tagset.getType().getType().equals("span")) {
                    spanLayers.add(tagset);
                }

            }

            if (selectedSpanId != -1) {
                tagSetsModel = new Model<TagSet>(selectedtTagSet);
                tagSetsModel = new Model<TagSet>(selectedtTagSet);
                Tag tag;
                try {
                    tag = annotationService.getTag(TypeUtil.getLabel(selectedSpanType),
                            selectedtTagSet);
                    tagsModel = new Model<String>(tag.getName());
                }
                catch (Exception e) {// It is a tag which is not in the tag list.
                    tagsModel = new Model<String>("");
                }
            }
            else if (bratAnnotatorModel.getRememberedSpanTagSet() != null
                    && conatinsTagSet(bratAnnotatorModel.getAnnotationLayers(),
                            bratAnnotatorModel.getRememberedSpanTagSet())) {
                selectedtTagSet = bratAnnotatorModel.getRememberedSpanTagSet();
                tagSetsModel = new Model<TagSet>(selectedtTagSet);
                tagsModel = new Model<String>(bratAnnotatorModel.getRememberedSpanTag().getName());
                // for lemma,
                if (tagsModel.getObject() == null) {
                    tagsModel.setObject(selectedText);
                }
            }
            else {
                selectedtTagSet = ((TagSet) spanLayers.get(0));
                tagSetsModel = new Model<TagSet>(selectedtTagSet);
                tagsModel = new Model<String>("");

                if (selectedtTagSet.getType().getName().equals(AnnotationTypeConstant.LEMMA)) {
                    tagsModel.setObject(selectedText);
                }
            }

            add(new Label("selectedText", selectedText));

            tags = new ComboBox<Tag>("tags", tagsModel,
                    annotationService.listTags(selectedtTagSet), new ComboBoxRenderer<Tag>("name",
                            "name"));
            add(tags);

            add(new DropDownChoice<TagSet>("tagSets", tagSetsModel, spanLayers)
            {
                private static final long serialVersionUID = -508831184292402704L;

                @Override
                protected void onSelectionChanged(TagSet aNewSelection)
                {
                    selectedtTagSet = aNewSelection;
                    if (isLemma(aNewSelection)) {
                        tagsModel.setObject(selectedText);
                    }
                    else {
                        tagsModel.setObject("");
                    }

                    updateTagsComboBox();

                }

                @Override
                protected boolean wantOnSelectionChangedNotifications()
                {
                    return true;
                }

                @Override
                protected CharSequence getDefaultChoice(String aSelectedValue)
                {
                    return "";
                }

            }.setChoiceRenderer(new ChoiceRenderer<TagSet>()
            {
                private static final long serialVersionUID = 1L;

                @Override
                public Object getDisplayValue(TagSet aObject)
                {
                    return aObject.getName();
                }
            }).setOutputMarkupId(true));

            add(new AjaxButton("annotate")
            {
                private static final long serialVersionUID = 980971048279862290L;

                @Override
                protected void onSubmit(AjaxRequestTarget aTarget, Form<?> form)
                {
                    BratAjaxCasController controller = new BratAjaxCasController(repository,
                            annotationService);
                    try {
                        JCas jCas = getCas(bratAnnotatorModel);

                        String annotationType = "";

                        if (tags.getModelObject() == null) {
                            aTarget.add(feedbackPanel);
                            error("No Tag is selected!");
                        }
                        else if (!annotationService.existsTag(tags.getModelObject(),
                                selectedtTagSet) && !isLemma(selectedtTagSet)) {
                            aTarget.add(feedbackPanel);
                            error(tags.getModelObject()
                                    + " is not in the tag list. Please choose form the existing tags");
                        }
                        else {

                            Tag selectedTag;
                            if (isLemma(selectedtTagSet)) {
                                selectedTag = new Tag();
                                annotationType = tags.getModelObject();
                            }
                            else {
                                selectedTag = (Tag) annotationService.getTag(tags.getModelObject(),
                                        selectedtTagSet);
                                annotationType = TypeUtil.getQualifiedLabel(selectedTag);
                            }

                            controller.createSpanAnnotation(jCas, beginOffset, endOffset,
                                    annotationType, null, null);
                            repository.updateJCas(bratAnnotatorModel.getMode(),
                                    bratAnnotatorModel.getDocument(), bratAnnotatorModel.getUser(),
                                    jCas);

                            // update timestamp now
                            AnnotationDocument annotationDocument = repository
                                    .getAnnotationDocument(bratAnnotatorModel.getDocument(),
                                            bratAnnotatorModel.getUser());
                            repository.updateTimeStamp(annotationDocument);

                            if (bratAnnotatorModel.isScrollPage()) {
                                updateSentenceAddressAndOffsets(jCas, beginOffset);
                            }

                            bratAnnotatorModel.setRememberedSpanTagSet(selectedtTagSet);
                            bratAnnotatorModel.setRememberedSpanTag(selectedTag);
                            bratAnnotatorModel.setMessage("The span annotation ["
                                    + TypeUtil.getLabel(annotationType) + "] is added");

                            // A hack to rememeber the Visural DropDown display value
                            HttpSession session = ((ServletWebRequest) RequestCycle.get()
                                    .getRequest()).getContainerRequest().getSession();
                            session.setAttribute("model", bratAnnotatorModel);
                            aModalWindow.close(aTarget);
                        }
                    }
                    catch (UIMAException e) {
                        aTarget.add(feedbackPanel);
                        error(ExceptionUtils.getRootCauseMessage(e));
                    }
                    catch (ClassNotFoundException e) {
                        aTarget.add(feedbackPanel);
                        error(e.getMessage());
                    }
                    catch (IOException e) {
                        aTarget.add(feedbackPanel);
                        error(e.getMessage());
                    }
                    catch (BratAnnotationException e) {
                        aTarget.add(feedbackPanel);
                        error(e.getMessage());
                    }
                }

            }.add(new Behavior()
            {
                private static final long serialVersionUID = -3612493911620740735L;

                @Override
                public void renderHead(Component component, IHeaderResponse response)
                {
                    super.renderHead(component, response);
                    response.renderOnLoadJavaScript("$('#" + component.getMarkupId()
                            + "').focus();");
                }
            }));

            add(new AjaxSubmitLink("delete")
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {

                    BratAjaxCasController controller = new BratAjaxCasController(repository,
                            annotationService);
                    try {
                        JCas jCas = getCas(bratAnnotatorModel);

                        TypeAdapter adapter = getAdapter(selectedtTagSet.getType());
                        if (!adapter.isDeletable()) {
                            aTarget.add(feedbackPanel);
                            error("This annotation can't be deleted!");
                        }
                        else {
                            controller.deleteAnnotation(jCas, selectedSpanId);
                            repository.updateJCas(bratAnnotatorModel.getMode(),
                                    bratAnnotatorModel.getDocument(), bratAnnotatorModel.getUser(),
                                    jCas);
                            // update timestamp now
                            AnnotationDocument annotationDocument = repository
                                    .getAnnotationDocument(bratAnnotatorModel.getDocument(),
                                            bratAnnotatorModel.getUser());
                            repository.updateTimeStamp(annotationDocument);

                            if (bratAnnotatorModel.isScrollPage()) {
                                updateSentenceAddressAndOffsets(jCas, beginOffset);
                            }

                            bratAnnotatorModel.setMessage("The span annotation ["
                                    + selectedSpanType + "] is deleted");

                            // A hack to rememeber the Visural DropDown display value
                            HttpSession session = ((ServletWebRequest) RequestCycle.get()
                                    .getRequest()).getContainerRequest().getSession();
                            session.setAttribute("model", bratAnnotatorModel);
                            aModalWindow.close(aTarget);
                        }
                    }
                    catch (UIMAException e) {
                        aTarget.add(feedbackPanel);
                        error(ExceptionUtils.getRootCauseMessage(e));
                    }
                    catch (ClassNotFoundException e) {
                        aTarget.add(feedbackPanel);
                        error(e.getMessage());
                    }
                    catch (IOException e) {
                        aTarget.add(feedbackPanel);
                        error(e.getMessage());
                    }
                }

                @Override
                public boolean isVisible()
                {
                    return isModify;
                }
            });
        }

        private void updateTagsComboBox()
        {
            tags.remove();
            tags = new ComboBox<Tag>("tags", tagsModel,
                    annotationService.listTags(selectedtTagSet), new ComboBoxRenderer<Tag>("name",
                            "name"));
            add(tags);
        }
    }

    private void updateSentenceAddressAndOffsets(JCas jCas, int start)
    {
        int address = BratAjaxCasUtil.selectSentenceAt(jCas,
                bratAnnotatorModel.getSentenceBeginOffset(),
                bratAnnotatorModel.getSentenceEndOffset()).getAddress();
        bratAnnotatorModel.setSentenceAddress(BratAjaxCasUtil.getSentenceBeginAddress(jCas,
                address, start, bratAnnotatorModel.getProject(), bratAnnotatorModel.getDocument(),
                bratAnnotatorModel.getWindowSize()));

        Sentence sentence = selectByAddr(jCas, Sentence.class,
                bratAnnotatorModel.getSentenceAddress());
        bratAnnotatorModel.setSentenceBeginOffset(sentence.getBegin());
        bratAnnotatorModel.setSentenceEndOffset(sentence.getEnd());
    }

    private JCas getCas(BratAnnotatorModel aBratAnnotatorModel)
        throws UIMAException, IOException, ClassNotFoundException
    {

        if (aBratAnnotatorModel.getMode().equals(Mode.ANNOTATION)
                || aBratAnnotatorModel.getMode().equals(Mode.CORRECTION)
                || aBratAnnotatorModel.getMode().equals(Mode.CORRECTION_MERGE)) {

            return repository.readJCas(aBratAnnotatorModel.getDocument(),
                    aBratAnnotatorModel.getProject(), aBratAnnotatorModel.getUser());
        }
        else {
            return repository.getCurationDocumentContent(bratAnnotatorModel.getDocument());
        }
    }

    public class SelectionModel
        implements Serializable
    {
        private static final long serialVersionUID = -4178958678920895292L;
        public TagSet tagSets;
        public Tag tags;
        public String selectedText;
    }

    public SpanAnnotationModalWindowPage(ModalWindow modalWindow,
            BratAnnotatorModel aBratAnnotatorModel, String aSelectedText, int aBeginOffset,
            int aEndOffset)
    {
        this.beginOffset = aBeginOffset;
        this.endOffset = aEndOffset;

        this.selectedText = aSelectedText;

        this.bratAnnotatorModel = aBratAnnotatorModel;
        this.annotationDialogForm = new AnnotationDialogForm("annotationDialogForm", modalWindow);
        add(annotationDialogForm);
    }

    public SpanAnnotationModalWindowPage(ModalWindow modalWindow,
            BratAnnotatorModel aBratAnnotatorModel, String aSelectedText, int aBeginOffset,
            int aEndOffset, String aType, int selectedSpanId)
    {
        this.selectedSpanId = selectedSpanId;
        this.selectedSpanType = TypeUtil.getLabel(aType);

        String layerName = TypeUtil.getSpanLayerName(TypeUtil.getLabelPrefix(aType));

        AnnotationType layer = this.annotationService.getType(layerName, "span");

        this.selectedtTagSet = this.annotationService.getTagSet(layer,
                aBratAnnotatorModel.getProject());

        this.beginOffset = aBeginOffset;
        this.endOffset = aEndOffset;

        this.selectedText = aSelectedText;

        this.bratAnnotatorModel = aBratAnnotatorModel;
        this.annotationDialogForm = new AnnotationDialogForm("annotationDialogForm", modalWindow);
        add(annotationDialogForm);
        this.isModify = true;
    }

    private boolean conatinsTagSet(Set<TagSet> aTagSets, TagSet aTagSet)
    {
        for (TagSet tagSet : aTagSets) {
            if (tagSet.getId() == aTagSet.getId()) {
                return true;
            }
        }
        return false;
    }

    private boolean isLemma(TagSet aTagSet)
    {
        if (aTagSet.getType().getName().equals(AnnotationTypeConstant.LEMMA)) {
            return true;
        }
        else {
            return false;
        }
    }
}