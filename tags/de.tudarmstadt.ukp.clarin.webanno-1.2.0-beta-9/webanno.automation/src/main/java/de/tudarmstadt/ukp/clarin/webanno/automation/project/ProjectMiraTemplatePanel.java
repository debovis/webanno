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
package de.tudarmstadt.ukp.clarin.webanno.automation.project;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import com.googlecode.wicket.jquery.ui.form.button.IndicatingAjaxButton;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.api.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.automation.util.AutomationUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AutomationStatus;
import de.tudarmstadt.ukp.clarin.webanno.model.MiraTemplate;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Status;
import de.tudarmstadt.ukp.clarin.webanno.support.EntityModel;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * A Panel used to define automation properties for the {@link MIRA} machine learning algorithm
 *
 * @author Seid Muhie Yimam
 *
 */
public class ProjectMiraTemplatePanel
    extends Panel
{
    private static final long serialVersionUID = 2116717853865353733L;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "userRepository")
    private UserDao userRepository;

    private final MiraTrainLayerSelectionForm miraTrainLayerSelectionForm;
    private final MiraTemplateDetailForm miraTemplateDetailForm;
    @SuppressWarnings("unused")
    private final OtherFeatureDeatilForm otherFeatureDeatilForm;
    private ProjectTrainingDocumentsPanel trainFeatureDocumentsPanel;
    private ProjectTrainingDocumentsPanel otherTrainFeatureDocumentsPanel;

    private boolean isLayerDetail = true;
    private final Model<Project> selectedProjectModel;

    private Model<AnnotationFeature> featureModel = new Model<AnnotationFeature>();
    private Model<AnnotationFeature> otherFeatureModel = new Model<AnnotationFeature>();

    private AnnotationFeature selectedFeature;
    private MiraTemplate templaet = new MiraTemplate();

    @SuppressWarnings("unused")
    private final ApplyForm applyForm;
    private DropDownChoice<AnnotationFeature> features;
    private DropDownChoice<AnnotationFeature> otherFeatures;

    public ProjectMiraTemplatePanel(String id, final Model<Project> aProjectModel)
    {
        super(id);
        this.selectedProjectModel = aProjectModel;
        for (MiraTemplate template : repository.listMiraTemplates(selectedProjectModel.getObject())) {
            if (template.isCurrentLayer()) {
                this.templaet = template;
                selectedFeature = template.getTrainFeature();
                break;
            }
        }
        featureModel.setObject(selectedFeature);
        miraTrainLayerSelectionForm = new MiraTrainLayerSelectionForm("miraTrainLayerSelectionForm");
        add(miraTrainLayerSelectionForm);

        add(miraTemplateDetailForm = new MiraTemplateDetailForm("miraTemplateDetailForm")
        {
            private static final long serialVersionUID = -4722848235169124717L;

            @Override
            public boolean isVisible()
            {
                return selectedFeature != null && isLayerDetail;
            }
        });
        miraTemplateDetailForm.setModelObject(templaet);

        add(otherFeatureDeatilForm = new OtherFeatureDeatilForm("otherFeatureDeatilForm")
        {
            private static final long serialVersionUID = 3192960675893574547L;

            @Override
            public boolean isVisible()
            {
                return selectedFeature != null && !isLayerDetail;
            }
        });

        add(trainFeatureDocumentsPanel = new ProjectTrainingDocumentsPanel(
                "trainFeatureDocumentsPanel", aProjectModel, featureModel)
        {

            private static final long serialVersionUID = 7698999083009818310L;

            @Override
            public boolean isVisible()
            {
                return selectedFeature != null && isLayerDetail;
            }
        });
        trainFeatureDocumentsPanel.setOutputMarkupPlaceholderTag(true);

        add(otherTrainFeatureDocumentsPanel = new ProjectTrainingDocumentsPanel(
                "otherTrainFeatureDocumentsPanel", aProjectModel, otherFeatureModel)
        {
            private static final long serialVersionUID = -4663938706290521594L;

            @Override
            public boolean isVisible()
            {
                return selectedFeature != null && !isLayerDetail;
            }
        });
        otherTrainFeatureDocumentsPanel.setOutputMarkupPlaceholderTag(true);

        add(applyForm = new ApplyForm("applyForm")
        {
            private static final long serialVersionUID = 3866085992209480718L;

            @Override
            protected void onConfigure()
            {
                super.onConfigure();
                if (templaet.getId() == 0) {
                    this.setVisible(false);
                }
                else {
                    this.setVisible(true);
                }
            }
        });

    }

    private class MiraTrainLayerSelectionForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1528847861284911270L;

        public MiraTrainLayerSelectionForm(String id)
        {
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));
            final Project project = selectedProjectModel.getObject();

            add(features = new DropDownChoice<AnnotationFeature>("features")
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<AnnotationFeature>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<AnnotationFeature> load()
                        {
                            List<AnnotationFeature> allFeatures = annotationService
                                    .listAnnotationFeature(project);
                            List<AnnotationFeature> spanFeatures = new ArrayList<AnnotationFeature>();

                            for (AnnotationFeature feature : allFeatures) {
                                if (!feature.getLayer().isEnabled()
                                        || feature.getLayer().getName()
                                                .equals(Token.class.getName())
                                        || feature.getLayer().getName()
                                                .equals(Lemma.class.getName())) {
                                    continue;
                                }
                                if (feature.getLayer().getType().equals(WebAnnoConst.SPAN_TYPE)) {
                                    spanFeatures.add(feature);
                                }
                            }
                            return spanFeatures;
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<AnnotationFeature>()
                    {
                        private static final long serialVersionUID = -2000622431037285685L;

                        @Override
                        public Object getDisplayValue(AnnotationFeature aObject)
                        {
                            return "[ " + aObject.getLayer().getUiName() + "] "
                                    + aObject.getUiName();
                        }
                    });
                    setNullValid(false);
                }

                @Override
                public void onSelectionChanged(AnnotationFeature aNewSelection)
                {
                    selectedFeature = (AnnotationFeature) aNewSelection;
                    if (repository.existsMiraTemplate(selectedFeature)) {
                        templaet = repository.getMiraTemplate(selectedFeature);
                    }
                    else {
                        templaet = new MiraTemplate();
                        templaet.setTrainFeature((AnnotationFeature) aNewSelection);
                    }
                    featureModel.setObject(selectedFeature);
                    isLayerDetail = true;
                    updateTrainFeatureDocumentsPanel(trainFeatureDocumentsPanel);
                    updateTrainFeatureDocumentsPanel(otherTrainFeatureDocumentsPanel);
                    miraTemplateDetailForm.setModelObject(templaet);
                    // Since the automation layer is changed, all non-training document should be
                    // re-annotated
                    for (SourceDocument sd : repository.listSourceDocuments(project)) {
                        if (!sd.isTrainingDocument()) {
                            sd.setProcessed(false);
                        }
                    }
                }

                @Override
                protected boolean wantOnSelectionChangedNotifications()
                {
                    return true;
                }
            }).setOutputMarkupId(true);
            features.setModelObject(selectedFeature);
        }

    }

    private class MiraTemplateDetailForm
        extends Form<MiraTemplate>
    {
        private static final long serialVersionUID = -683824912741426241L;

        public MiraTemplateDetailForm(String id)
        {
            super(id, new CompoundPropertyModel<MiraTemplate>(new EntityModel<MiraTemplate>(
                    new MiraTemplate())));

            add(new CheckBox("predictInThisPage"));

            add(new CheckBox("annotateAndPredict"));

            add(new Button("save", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    templaet = MiraTemplateDetailForm.this.getModelObject();
                    if (templaet.getId() == 0) {
                        templaet.setTrainFeature(selectedFeature);
                        repository.createTemplate(templaet);
                        featureModel.setObject(MiraTemplateDetailForm.this.getModelObject()
                                .getTrainFeature());
                    }
                    templaet.setCurrentLayer(true);
                    for (MiraTemplate template : repository.listMiraTemplates(selectedProjectModel
                            .getObject())) {
                        if (template.equals(templaet)) {
                            continue;
                        }
                        if (template.isCurrentLayer()) {
                            template.setCurrentLayer(false);
                        }
                    }
                }
            });
        }
    }

    /**
     * {@link AnnotationFeature} used as a feature for the current training layer
     *
     */
    private class OtherFeatureDeatilForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -683824912741426241L;

        public OtherFeatureDeatilForm(String id)
        {
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));

            add(otherFeatures = new DropDownChoice<AnnotationFeature>("features")
            {
                private static final long serialVersionUID = -1923453084703805794L;

                {
                    setNullValid(false);
                    setChoices(new LoadableDetachableModel<List<AnnotationFeature>>()
                    {
                        private static final long serialVersionUID = -6376636005341159307L;

                        @Override
                        protected List<AnnotationFeature> load()
                        {
                            List<AnnotationFeature> features = annotationService
                                    .listAnnotationFeature(selectedProjectModel.getObject());
                            features.remove(miraTemplateDetailForm.getModelObject()
                                    .getTrainFeature());
                            features.removeAll(miraTemplateDetailForm.getModelObject()
                                    .getOtherFeatures());
                            for (AnnotationFeature feature : annotationService
                                    .listAnnotationFeature(selectedProjectModel.getObject())) {
                                if (!feature.getLayer().isEnabled()
                                        || !feature.getLayer().getType()
                                                .equals(WebAnnoConst.SPAN_TYPE)
                                        || feature.getLayer().getName()
                                                .equals(Lemma.class.getName())
                                        || feature.getLayer().getName()
                                                .equals(Token.class.getName())) {
                                    features.remove(feature);
                                }
                            }
                            return features;
                        }
                    });

                    setChoiceRenderer(new ChoiceRenderer<AnnotationFeature>()
                    {
                        private static final long serialVersionUID = 4607720784161484145L;

                        @Override
                        public Object getDisplayValue(AnnotationFeature aObject)
                        {
                            return "[ " + aObject.getLayer().getUiName() + "] "
                                    + aObject.getUiName();
                        }
                    });
                }

                @Override
                protected void onSelectionChanged(AnnotationFeature aNewSelection)
                {
                    miraTemplateDetailForm.getModelObject().getOtherFeatures().add(aNewSelection);
                }

                @Override
                protected boolean wantOnSelectionChangedNotifications()
                {
                    return true;
                }
            });
            otherFeatures.setModelObject(null);// always force to choose, even after selection of
                                               // feature

            add(new ListChoice<AnnotationFeature>("selectedFeatures")
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<AnnotationFeature>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<AnnotationFeature> load()
                        {
                            return new ArrayList<AnnotationFeature>(miraTemplateDetailForm
                                    .getModelObject().getOtherFeatures());
                        }
                    });

                    setChoiceRenderer(new ChoiceRenderer<AnnotationFeature>()
                    {
                        private static final long serialVersionUID = 4607720784161484145L;

                        @Override
                        public Object getDisplayValue(AnnotationFeature aObject)
                        {
                            return "[ " + aObject.getLayer().getUiName() + "] "
                                    + aObject.getUiName();
                        }
                    });
                    setNullValid(false);
                }

                @Override
                protected void onSelectionChanged(AnnotationFeature aNewSelection)
                {
                    otherFeatureModel.setObject(aNewSelection);
                    updateOtherFeatureDocumentsPanel(otherTrainFeatureDocumentsPanel);
                    otherFeatures.setModelObject(null);// always force to choose, even after
                                                       // selection of feature
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
            });
        }
    }

    @SuppressWarnings("rawtypes")
    private class ApplyForm
        extends Form
    {
        private static final long serialVersionUID = -683824912741426241L;

        public ApplyForm(String id)
        {
            super(id);

            add(new IndicatingAjaxButton("apply", new ResourceModel("label"))
            {

                private static final long serialVersionUID = 1L;

                @Override
                protected void onSubmit(AjaxRequestTarget aTarget, Form<?> form)
                {
                    MiraTemplate template = miraTemplateDetailForm.getModelObject();
                    AutomationStatus automationStatus = new AutomationStatus();
                    try {

                        // no training document is added / no curation is done yet!
                        boolean existsTrainDocument = false;
                        for (SourceDocument document : repository
                                .listSourceDocuments(selectedProjectModel.getObject())) {
                            if (document.getState().equals(SourceDocumentState.CURATION_FINISHED)
                                    || (document.isTrainingDocument() && template.getTrainFeature()
                                            .equals(document.getFeature()))) {
                                existsTrainDocument = true;
                                break;
                            }
                        }
                        if (!existsTrainDocument) {
                            error("No training document exists to proceed.");
                            return;
                        }
                        if (!template.isCurrentLayer()) {
                            error("Please save automation layer details to proceed.");
                            return;
                        }

                        // no need to re-train if no new document is added
                        boolean existUnprocessedDocument = false;
                        ;
                        for (SourceDocument document : repository
                                .listSourceDocuments(selectedProjectModel.getObject())) {
                            if (!document.isProcessed()) {
                                existUnprocessedDocument = true;
                                break;
                            }
                        }
                        if (!existUnprocessedDocument) {
                            error("No new training/annotation document added.");
                            return;
                        }

                        int annodoc = 0, trainDoc = 0;

                        for (SourceDocument document : repository
                                .listSourceDocuments(selectedProjectModel.getObject())) {
                            if ((document.isTrainingDocument() || document.getState().equals(
                                    SourceDocumentState.CURATION_FINISHED))
                                    && !document.isProcessed()) {
                                trainDoc++;
                            }
                            else if(!document.isTrainingDocument() && !document.isProcessed()) {
                                annodoc++;
                            }
                        }

                        automationStatus = repository.existsAutomationStatus(template) ? repository
                                .getAutomationStatus(template) : automationStatus;
                        automationStatus.setStartime(new Timestamp(new Date().getTime()));
                        automationStatus.setEndTime(new Timestamp(new Date().getTime()));
                        automationStatus.setTrainDocs(trainDoc);
                        automationStatus.setAnnoDocs(annodoc);
                        automationStatus.setTotalDocs(annodoc + trainDoc);
                        automationStatus.setTemplate(template);

                        repository.createAutomationStatus(automationStatus);

                        template.setAutomationStarted(true);

                        automationStatus.setStatus(Status.GENERATE_TRAIN_DOC);
                        AutomationUtil.addOtherFeatureTrainDocument(template, repository);
                        AutomationUtil.otherFeatureClassifiers(template, repository);

                        AutomationUtil.generateTrainDocument(template, repository, true);
                        AutomationUtil.generatePredictDocument(template, repository);

                        automationStatus.setStatus(Status.GENERATE_CLASSIFIER);
                        miraTemplateDetailForm.getModelObject().setResult(
                                AutomationUtil.generateFinalClassifier(template, repository));
                        AutomationUtil.addOtherFeatureToPredictDocument(template, repository);

                        automationStatus.setStatus(Status.PREDICTION);
                        AutomationUtil.predict(template, repository);
                        template.setAutomationStarted(false);
                        repository.createTemplate(template);
                        automationStatus.setStatus(Status.COMPLETED);
                        automationStatus.setEndTime(new Timestamp(new Date().getTime()));

                    }
                    catch (UIMAException e) {
                        error(ExceptionUtils.getRootCause(e));
                    }
                    catch (ClassNotFoundException e) {
                        error(e.getMessage());
                    }
                    catch (IOException e) {
                        error(e.getMessage());
                    }
                    catch (BratAnnotationException e) {
                        error(e.getMessage());
                    }
                    finally {
                        automationStatus.setStatus(Status.COMPLETED);
                        automationStatus.setEndTime(new Timestamp(new Date().getTime()));
                        template.setAutomationStarted(false);
                        repository.createTemplate(template);
                    }
                }

                @Override
                public boolean isEnabled()
                {
                    return !miraTemplateDetailForm.getModelObject().isAutomationStarted();
                }
            });

            add(new Button("layerDetails", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    isLayerDetail = true;
                    updateTrainFeatureDocumentsPanel(trainFeatureDocumentsPanel);
                    updateTrainFeatureDocumentsPanel(otherTrainFeatureDocumentsPanel);
                }
            });

            add(new Button("addOtherLayer", new ResourceModel("label"))
            {
                private static final long serialVersionUID = 1L;

                @Override
                public void onSubmit()
                {
                    if (miraTemplateDetailForm.getModelObject().getId() == 0) {
                        error("Please save the training layer detail first");
                        return;
                    }
                    isLayerDetail = false;
                    updateTrainFeatureDocumentsPanel(trainFeatureDocumentsPanel);
                    updateTrainFeatureDocumentsPanel(otherTrainFeatureDocumentsPanel);
                    otherFeatures.setModelObject(null);// always force to choose, even after
                                                       // selection of feature
                }
            });
        }
    }

    void updateTrainFeatureDocumentsPanel(ProjectTrainingDocumentsPanel aDcumentsPanel)
    {
        trainFeatureDocumentsPanel.remove();
        add(trainFeatureDocumentsPanel = new ProjectTrainingDocumentsPanel(
                "trainFeatureDocumentsPanel", selectedProjectModel, featureModel)
        {

            private static final long serialVersionUID = 7698999083009818310L;

            @Override
            public boolean isVisible()
            {
                return selectedFeature.getId() != 0 && isLayerDetail;
            }
        });
    }

    void updateOtherFeatureDocumentsPanel(ProjectTrainingDocumentsPanel aDcumentsPanel)
    {
        otherTrainFeatureDocumentsPanel.remove();
        add(otherTrainFeatureDocumentsPanel = new ProjectTrainingDocumentsPanel(
                "otherTrainFeatureDocumentsPanel", selectedProjectModel, otherFeatureModel)
        {

            private static final long serialVersionUID = 7698999083009818310L;

            @Override
            public boolean isVisible()
            {
                return selectedFeature.getId() != 0 && !isLayerDetail;
            }
        });
    }

    public class SelectionModel
        implements Serializable
    {
        private static final long serialVersionUID = -4905538356691404575L;
        public AnnotationFeature features = new AnnotationFeature();
        public AnnotationFeature selectedFeatures = new AnnotationFeature();

    }
}