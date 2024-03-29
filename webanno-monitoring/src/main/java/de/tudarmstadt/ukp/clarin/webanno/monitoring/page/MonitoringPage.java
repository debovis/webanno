/*******************************************************************************
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universit?t Darmstadt
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
package de.tudarmstadt.ukp.clarin.webanno.monitoring.page;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition.ANNOTATION_FINISHED_TO_ANNOTATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition.ANNOTATION_IN_PROGRESS_TO_ANNOTATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition.IGNORE_TO_NEW;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition.NEW_TO_ANNOTATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition.NEW_TO_IGNORE;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition.CURATION_FINISHED_TO_CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition.CURATION_IN_PROGRESS_TO_CURATION_FINISHED;

import java.awt.Color;
import java.io.IOException;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.DataGridView;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.ContextRelativeResource;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnits;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.RectangleInsets;
import org.jfree.util.UnitType;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.SecurityUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.CurationPanel;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateTransition;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Authority;
import de.tudarmstadt.ukp.clarin.webanno.model.MiraTemplate;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentStateTransition;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.monitoring.support.ChartImageResource;
import de.tudarmstadt.ukp.clarin.webanno.monitoring.support.DynamicColumnMetaData;
import de.tudarmstadt.ukp.clarin.webanno.monitoring.support.EmbeddableImage;
import de.tudarmstadt.ukp.clarin.webanno.monitoring.support.TableDataProvider;
import de.tudarmstadt.ukp.clarin.webanno.monitoring.support.TwoPairedKappa;
import de.tudarmstadt.ukp.clarin.webanno.support.EntityModel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.home.page.ApplicationPageBase;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.statistics.agreement.TwoRaterKappaAgreement;

/**
 * A Page To display different monitoring and statistics measurements tabularly and graphically.
 *
 * @author Seid Muhie Yimam
 *
 */
public class MonitoringPage
    extends ApplicationPageBase
{
    private static final Log LOG = LogFactory.getLog(DocumentStatusColumnMetaData.class);
    
    private static final long serialVersionUID = -2102136855109258306L;

    private static final int CHART_WIDTH = 300;

    /**
     * The user column in the user-document status table
     */
    public static final String USER = "user:";

    /**
     * The document column in the user-document status table
     */
    public static final String DOCUMENT = "document:";

    public static final String CURATION = "curation";

    public static final String LAST_ACCESS = "last access:";
    public static final String LAST_ACCESS_ROW = "last access";

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    private final ProjectSelectionForm projectSelectionForm;
    private final MonitoringDetailForm monitoringDetailForm;
    private final Image annotatorsProgressImage;
    private final Image annotatorsProgressPercentageImage;
    private final Image overallProjectProgressImage;
    private  TrainingResultForm trainingResultForm;

    private Label overview;
    private DefaultDataTable<?,?> annotationDocumentStatusTable;
    private DefaultDataTable<?,?> agreementTable;
    private final Label projectName;
    private AgreementForm agreementForm;
    private final AnnotationTypeSelectionForm annotationTypeSelectionForm;
    private ListChoice<AnnotationFeature> features;
    private transient Map<SourceDocument, Map<User, JCas>> documentJCases;

    private String result;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public MonitoringPage()
        throws UIMAException, IOException, ClassNotFoundException
    {
        projectSelectionForm = new ProjectSelectionForm("projectSelectionForm");

        monitoringDetailForm = new MonitoringDetailForm("monitoringDetailForm");

        agreementForm = new AgreementForm("agreementForm", new Model<AnnotationLayer>(),
                new Model<Project>());
        agreementForm.setVisible(false);
        add(agreementForm);

        trainingResultForm = new TrainingResultForm("trainingResultForm");
        trainingResultForm.setVisible(false);
        add(trainingResultForm);

        annotationTypeSelectionForm = new AnnotationTypeSelectionForm("annotationTypeSelectionForm");
        annotationTypeSelectionForm.setVisible(false);
        add(annotationTypeSelectionForm);

        annotatorsProgressImage = new NonCachingImage("annotator");
        annotatorsProgressImage.setOutputMarkupPlaceholderTag(true);
        annotatorsProgressImage.setVisible(false);

        annotatorsProgressPercentageImage = new NonCachingImage("annotatorPercentage");
        annotatorsProgressPercentageImage.setOutputMarkupPlaceholderTag(true);
        annotatorsProgressPercentageImage.setVisible(false);

        overallProjectProgressImage = new NonCachingImage("overallProjectProgressImage");
        final Map<String, Integer> overallProjectProgress = getOverallProjectProgress();
        overallProjectProgressImage.setImageResource(createProgressChart(overallProjectProgress,
                100, true));
        overallProjectProgressImage.setOutputMarkupPlaceholderTag(true);
        overallProjectProgressImage.setVisible(true);
        add(overallProjectProgressImage);
        add(overview = new Label("overview", "overview of projects"));

        add(projectSelectionForm);
        projectName = new Label("projectName", "");

        Project project = repository.listProjects().get(0);

        List<List<String>> userAnnotationDocumentLists = new ArrayList<List<String>>();
        List<SourceDocument> dc = repository.listSourceDocuments(project);
        List<SourceDocument> trainingDoc = new ArrayList<SourceDocument>();
        for (SourceDocument sdc : dc) {
            if (sdc.isTrainingDocument()) {
                trainingDoc.add(sdc);
            }
        }
        dc.removeAll(trainingDoc);
        for (int j = 0; j < repository.listProjectUsersWithPermissions(project).size(); j++) {
            List<String> userAnnotationDocument = new ArrayList<String>();
            userAnnotationDocument.add("");
            for (int i = 0; i < dc.size(); i++) {
                userAnnotationDocument.add("");
            }
            userAnnotationDocumentLists.add(userAnnotationDocument);
        }
        List<String> documentListAsColumnHeader = new ArrayList<String>();
        documentListAsColumnHeader.add("Users");
        for (SourceDocument d : dc) {
            documentListAsColumnHeader.add(d.getName());
        }
        TableDataProvider prov = new TableDataProvider(documentListAsColumnHeader,
                userAnnotationDocumentLists);

        List<IColumn<?,?>> cols = new ArrayList<IColumn<?,?>>();

        for (int i = 0; i < prov.getColumnCount(); i++) {
            cols.add(new DocumentStatusColumnMetaData(prov, i, new Project(), repository));
        }
        annotationDocumentStatusTable = new DefaultDataTable("rsTable", cols, prov, 2);
        monitoringDetailForm.setVisible(false);
        add(monitoringDetailForm.add(annotatorsProgressImage)
                .add(annotatorsProgressPercentageImage).add(projectName)
                .add(annotationDocumentStatusTable));
        annotationDocumentStatusTable.setVisible(false);

    }

    private class ProjectSelectionForm
        extends Form<ProjectSelectionModel>
    {
        private static final long serialVersionUID = -1L;

        public ProjectSelectionForm(String id)
        {
            super(id, new CompoundPropertyModel<ProjectSelectionModel>(new ProjectSelectionModel()));

            add(new ListChoice<Project>("project")
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<Project>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<Project> load()
                        {
                            List<Project> allowedProject = new ArrayList<Project>();

                            String username = SecurityContextHolder.getContext()
                                    .getAuthentication().getName();
                            User user = repository.getUser(username);

                            List<Project> allProjects = repository.listProjects();
                            List<Authority> authorities = repository.listAuthorities(user);

                            // if global admin, show all projects
                            for (Authority authority : authorities) {
                                if (authority.getAuthority().equals("ROLE_ADMIN")) {
                                    return allProjects;
                                }
                            }

                            // else only projects she is admin of
                            for (Project project : allProjects) {
                                if (SecurityUtil.isProjectAdmin(project, repository, user)
                                        || SecurityUtil.isCurator(project, repository, user)) {
                                    allowedProject.add(project);
                                }
                            }
                            return allowedProject;
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<Project>("name"));
                    setNullValid(false);
                }

                @SuppressWarnings({ "unchecked", "rawtypes" })
                @Override
                protected void onSelectionChanged(Project aNewSelection)
                {
                    List<User> users = repository.listProjectUsersWithPermissions(aNewSelection,
                            PermissionLevel.USER);
                    List<SourceDocument> sourceDocuments = repository
                            .listSourceDocuments(aNewSelection);

                    List<SourceDocument> trainingDoc = new ArrayList<SourceDocument>();
                    for (SourceDocument sdc : sourceDocuments) {
                        if (sdc.isTrainingDocument()) {
                            trainingDoc.add(sdc);
                        }
                    }
                    sourceDocuments.removeAll(trainingDoc);

                    documentJCases = null;

                    if (aNewSelection == null) {
                        return;
                    }

                    monitoringDetailForm.setModelObject(aNewSelection);
                    monitoringDetailForm.setVisible(true);
                    annotationTypeSelectionForm.setVisible(true);
                    monitoringDetailForm.setVisible(true);

                    updateTrainingResultForm(aNewSelection);
                    result = "";

                    annotationTypeSelectionForm.setModelObject(new SelectionModel());
                    ProjectSelectionModel projectSelectionModel = ProjectSelectionForm.this.getModelObject();
                    projectSelectionModel.project = aNewSelection;
                    projectSelectionModel.annotatorsProgress = new TreeMap<String, Integer>();
                    projectSelectionModel.annotatorsProgressInPercent = new TreeMap<String, Integer>();
                    projectSelectionModel.totalDocuments = sourceDocuments.size();
                    updateAgreementForm();

                    ProjectSelectionForm.this.setVisible(true);

                    // Annotator's Progress
                    if (projectSelectionModel.project != null) {
                        projectSelectionModel.annotatorsProgressInPercent
                                .putAll(getPercentageOfFinishedDocumentsPerUser(projectSelectionModel.project));
                        projectSelectionModel.annotatorsProgress.putAll(getFinishedDocumentsPerUser(projectSelectionModel.project));

                    }
                    projectName.setDefaultModelObject(projectSelectionModel.project.getName());
                    overallProjectProgressImage.setVisible(false);
                    overview.setVisible(false);

                    annotatorsProgressImage.setImageResource(createProgressChart(
                            projectSelectionModel.annotatorsProgress,
                            projectSelectionModel.totalDocuments, false));
                    annotatorsProgressImage.setVisible(true);

                    annotatorsProgressPercentageImage.setImageResource(createProgressChart(
                            projectSelectionModel.annotatorsProgressInPercent, 100, true));
                    annotatorsProgressPercentageImage.setVisible(true);

                    List<String> documentListAsColumnHeader = new ArrayList<String>();
                    documentListAsColumnHeader.add("Documents");

                    // A column for curation user annotation document status
                    documentListAsColumnHeader.add(CURATION);

                    // List of users with USER permission level
                    List<User> usersWithPermissions = repository.listProjectUsersWithPermissions(
                            projectSelectionModel.project, PermissionLevel.USER);

                    for (User user : usersWithPermissions) {
                        documentListAsColumnHeader.add(user.getUsername());
                    }

                    List<List<String>> userAnnotationDocumentStatusList = new ArrayList<List<String>>();

                    // Add a timestamp row for every user.
                    List<String> projectTimeStamp = new ArrayList<String>();
                    projectTimeStamp.add(LAST_ACCESS + LAST_ACCESS_ROW); // first
                                                                         // column
                    if (repository.existsProjectTimeStamp(aNewSelection)) {
                        projectTimeStamp.add(LAST_ACCESS
                                + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(repository
                                        .getProjectTimeStamp(aNewSelection)));
                    }
                    else {
                        projectTimeStamp.add(LAST_ACCESS + "__");
                    }

                    for (User user : usersWithPermissions) {
                        if (repository.existsProjectTimeStamp(projectSelectionModel.project, user.getUsername())) {
                            projectTimeStamp.add(LAST_ACCESS
                                    + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(repository
                                            .getProjectTimeStamp(projectSelectionModel.project, user.getUsername())));
                        }
                        else {
                            projectTimeStamp.add(LAST_ACCESS + "__");
                        }
                    }

                    userAnnotationDocumentStatusList.add(projectTimeStamp);

                    for (SourceDocument document : sourceDocuments) {
                        List<String> userAnnotationDocuments = new ArrayList<String>();
                        userAnnotationDocuments.add(DOCUMENT + document.getName());

                        // Curation Document status
                        userAnnotationDocuments.add(CurationPanel.CURATION_USER + "-" + DOCUMENT
                                + document.getName());

                        for (User user : usersWithPermissions) {
                            // annotation document status for this annotator
                            userAnnotationDocuments.add(user.getUsername() + "-" + DOCUMENT
                                    + document.getName());
                        }

                        userAnnotationDocumentStatusList.add(userAnnotationDocuments);
                    }

                    TableDataProvider provider = new TableDataProvider(documentListAsColumnHeader,
                            userAnnotationDocumentStatusList);

                    List<IColumn<?,?>> columns = new ArrayList<IColumn<?,?>>();

                    for (int i = 0; i < provider.getColumnCount(); i++) {
                        columns.add(new DocumentStatusColumnMetaData(provider, i, projectSelectionModel.project,
                                repository));
                    }
                    annotationDocumentStatusTable.remove();
                    annotationDocumentStatusTable = new DefaultDataTable("rsTable", columns,
                            provider, 20);
                    annotationDocumentStatusTable.setOutputMarkupId(true);
                    monitoringDetailForm.add(annotationDocumentStatusTable);
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

    private class AnnotationTypeSelectionForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1L;

        public AnnotationTypeSelectionForm(String id)
        {
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));
            add(features = new ListChoice<AnnotationFeature>("features")
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<AnnotationFeature>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<AnnotationFeature> load()
                        {
                            List<AnnotationFeature> features = annotationService
                                    .listAnnotationFeature((projectSelectionForm.getModelObject().project));
                            List<AnnotationFeature> unusedFeatures = new ArrayList<AnnotationFeature>();
                            for (AnnotationFeature feature : features) {
                                if (feature.getLayer().getName().equals(Token.class.getName())
                                        || feature.getLayer().getName()
                                                .equals(WebAnnoConst.COREFERENCE_LAYER)) {
                                    unusedFeatures.add(feature);
                                }
                            }
                            features.removeAll(unusedFeatures);
                            return features;
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<AnnotationFeature>()
                    {
                        private static final long serialVersionUID = -3370671999669664776L;

                        @Override
                        public Object getDisplayValue(AnnotationFeature aObject)
                        {
                            return aObject.getLayer().getUiName()+" : " + aObject.getUiName();
                        }
                    });
                    setNullValid(false);

                }

                @Override
                protected CharSequence getDefaultChoice(String aSelectedValue)
                {
                    return "";
                }
            });
            features.add(new OnChangeAjaxBehavior()
            {
                private static final long serialVersionUID = 7492425689121761943L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    updateAgreementTable(aTarget);
                }
            }).setOutputMarkupId(true);
        }
    }

    Model<Project> projectModel = new Model<Project>()
    {
        private static final long serialVersionUID = -6394439155356911110L;

        @Override
        public Project getObject()
        {
            return projectSelectionForm.getModelObject().project;
        }
    };

    private Map<String, Integer> getFinishedDocumentsPerUser(Project aProject)
    {
        Map<String, Integer> annotatorsProgress = new HashMap<String, Integer>();
        if (aProject != null) {
            for (User user : repository.listProjectUsersWithPermissions(aProject)) {
                for (SourceDocument document : repository.listSourceDocuments(aProject)) {
                    if (repository.isAnnotationFinished(document, user)) {
                        if (annotatorsProgress.get(user.getUsername()) == null) {
                            annotatorsProgress.put(user.getUsername(), 1);
                        }
                        else {
                            int previousValue = annotatorsProgress.get(user.getUsername());
                            annotatorsProgress.put(user.getUsername(), previousValue + 1);
                        }
                    }
                }
                if (annotatorsProgress.get(user.getUsername()) == null) {
                    annotatorsProgress.put(user.getUsername(), 0);
                }
            }
        }
        return annotatorsProgress;
    }

    private Map<String, Integer> getPercentageOfFinishedDocumentsPerUser(Project aProject)
    {
        Map<String, Integer> annotatorsProgress = new HashMap<String, Integer>();
        if (aProject != null) {
            for (User user : repository.listProjectUsersWithPermissions(aProject)) {
                int finished = 0;
                int ignored = 0;
                int totalDocs = 0;
                List<SourceDocument> documents = repository.listSourceDocuments(aProject);
                List<SourceDocument> trainingDoc = new ArrayList<SourceDocument>();
                for (SourceDocument sdc : documents) {
                    if (sdc.isTrainingDocument()) {
                        trainingDoc.add(sdc);
                    }
                }
                documents.removeAll(trainingDoc);
                for (SourceDocument document : documents) {
                    totalDocs++;
                    if (repository.isAnnotationFinished(document, user)) {
                        finished++;
                    }
                    else if (repository.existsAnnotationDocument(document, user)) {
                        AnnotationDocument annotationDocument = repository.getAnnotationDocument(
                                document, user);
                        if (annotationDocument.getState().equals(AnnotationDocumentState.IGNORE)) {
                            ignored++;
                        }
                    }
                }
                annotatorsProgress.put(user.getUsername(),
                        (int) Math.round((double) (finished * 100) / (totalDocs - ignored)));
            }
        }
        return annotatorsProgress;
    }

    private Map<String, Integer> getOverallProjectProgress()
    {
        Map<String, Integer> overallProjectProgress = new LinkedHashMap<String, Integer>();
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = repository.getUser(username);
        for (Project project : repository.listProjects()) {
            if (SecurityUtil.isCurator(project, repository, user)
                    || SecurityUtil.isProjectAdmin(project, repository, user)) {
                int annoFinished = repository.listFinishedAnnotationDocuments(project).size();
                int allAnno = repository.numberOfExpectedAnnotationDocuments(project);
                int progress = (int) Math.round((double) (annoFinished * 100) / (allAnno));
                overallProjectProgress.put(project.getName(), progress);
            }
        }
        return overallProjectProgress;
    }

    static public class ProjectSelectionModel
        implements Serializable
    {
        protected int totalDocuments;

        private static final long serialVersionUID = -1L;

        public Project project;
        public Map<String, Integer> annotatorsProgress = new TreeMap<String, Integer>();
        public Map<String, Integer> annotatorsProgressInPercent = new TreeMap<String, Integer>();
    }

    static public class SelectionModel
        implements Serializable
    {
        private static final long serialVersionUID = -1L;

        public Project project;
        public AnnotationFeature features;
    }

    private class MonitoringDetailForm
        extends Form<Project>
    {
        private static final long serialVersionUID = -1L;

        public MonitoringDetailForm(String id)
        {
            super(id, new CompoundPropertyModel<Project>(new EntityModel<Project>(new Project())));

        }
    }

    @SuppressWarnings("rawtypes")
    private class AgreementForm
        extends Form<DefaultDataTable>
    {
        private static final long serialVersionUID = 344165080600348157L;

        @SuppressWarnings({ "unchecked" })
        public AgreementForm(String id, Model<AnnotationLayer> aType, Model<Project> aProject)

        {
            super(id);
            // Intialize the agreementTable with NOTHING.
            List<String> usersListAsColumnHeader = new ArrayList<String>();
            usersListAsColumnHeader.add("");
            List<String> agreementResult = new ArrayList<String>();
            agreementResult.add("");
            List<List<String>> agreementResults = new ArrayList<List<String>>();
            agreementResults.add(agreementResult);

            TableDataProvider provider = new TableDataProvider(usersListAsColumnHeader,
                    agreementResults);
            List<IColumn<?,?>> columns = new ArrayList<IColumn<?,?>>();

            for (int m = 0; m < provider.getColumnCount(); m++) {
                columns.add(new DynamicColumnMetaData(provider, m));
            }
            add(agreementTable = new DefaultDataTable("agreementTable", columns, provider, 10));
        }
    }

    private void updateAgreementForm()
    {
        agreementForm.remove();
        agreementForm = new AgreementForm("agreementForm", new Model<AnnotationLayer>(),
                new Model<Project>());
        add(agreementForm);
        agreementForm.setVisible(true);
    }
    
    private void updateTrainingResultForm(Project aProject)
    {
    	trainingResultForm.remove();
    	 trainingResultForm = new TrainingResultForm("trainingResultForm");
    	 add(trainingResultForm);
         trainingResultForm.setVisible(aProject.getMode().equals(Mode.AUTOMATION));
         
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void updateAgreementTable(AjaxRequestTarget aTarget)
    {

        Project project = projectSelectionForm.getModelObject().project;
        List<User> users = repository
                .listProjectUsersWithPermissions(project, PermissionLevel.USER);
        double[][] results = new double[users.size()][users.size()];
        if (features.getModelObject() != null) {

            TypeAdapter adapter = TypeUtil.getAdapter(features.getModelObject().getLayer());

            // assume all users finished only one document
            double[][] multipleDocumentsFinished = new double[users.size()][users.size()];
            for (int m = 0; m < users.size(); m++) {
                for (int j = 0; j < users.size(); j++) {
                    multipleDocumentsFinished[m][j] = 1.0;
                }
            }

            List<SourceDocument> sourceDocuments = repository.listSourceDocuments(project);
            List<SourceDocument> trainingDoc = new ArrayList<SourceDocument>();
            for (SourceDocument sdc : sourceDocuments) {
                if (sdc.isTrainingDocument()) {
                    trainingDoc.add(sdc);
                }
            }
            sourceDocuments.removeAll(trainingDoc);

            // a map that contains list of finished annotation documents for a
            // given user
            Map<User, List<SourceDocument>> finishedDocumentLists = new HashMap<User, List<SourceDocument>>();
            for (User user : users) {
                List<SourceDocument> finishedDocuments = new ArrayList<SourceDocument>();

                for (SourceDocument document : sourceDocuments) {
                    AnnotationDocument annotationDocument = repository.getAnnotationDocument(
                            document, user);
                    if (annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)) {
                        finishedDocuments.add(document);
                    }
                }
                finishedDocumentLists.put(user, finishedDocuments);
            }

            if (documentJCases == null) {
                documentJCases = getJCases(users, sourceDocuments);
            }
            
            results = computeKappa(users, adapter, features.getModelObject().getName(),
                    finishedDocumentLists, documentJCases);

            // Users with some annotations of this type

            List<String> usersListAsColumnHeader = new ArrayList<String>();
            usersListAsColumnHeader.add("users");

            for (User user : users) {
                usersListAsColumnHeader.add(user.getUsername());
            }
            List<List<String>> agreementResults = new ArrayList<List<String>>();
            int i = 0;
            for (User user1 : users) {
                List<String> agreementResult = new ArrayList<String>();
                agreementResult.add(user1.getUsername());

                for (int j = 0; j < users.size(); j++) {
                    if (j < i) {
                        agreementResult.add("");
                    }
                    else {
                        agreementResult.add((double) Math.round(results[i][j] * 100) / 100 + "");
                    }
                }
                i++;
                agreementResults.add(agreementResult);
            }

            TableDataProvider provider = new TableDataProvider(usersListAsColumnHeader,
                    agreementResults);

            List<IColumn<?,?>> columns = new ArrayList<IColumn<?,?>>();

            for (int m = 0; m < provider.getColumnCount(); m++) {
                columns.add(new DynamicColumnMetaData(provider, m));
            }
            agreementTable.remove();
            agreementTable = new DefaultDataTable("agreementTable", columns, provider, 10);
            agreementForm.add(agreementTable);
            aTarget.add(agreementForm);
        }
    }

    private class TrainingResultForm
        extends Form<ResultMOdel>
    {
        private static final long serialVersionUID = 1037668483966897381L;

        ListChoice<MiraTemplate> selectedTemplate;

        public TrainingResultForm(String id)
        {
            super(id, new CompoundPropertyModel<ResultMOdel>(new ResultMOdel()));

            add(new Label("resultLabel", new LoadableDetachableModel<String>()
            {
                private static final long serialVersionUID = 891566759811286173L;

                @Override
                protected String load()
                {
                    return result;

                }
            }).setOutputMarkupId(true));

            add(new Label("annoDocs", new LoadableDetachableModel<String>()
            {
                private static final long serialVersionUID = 891566759811286173L;

                @Override
                protected String load()
                {
                    MiraTemplate template = selectedTemplate.getModelObject();
                    if (template != null && repository.existsAutomationStatus(template)) {
                        return repository.getAutomationStatus(template).getAnnoDocs() + "";
                    }
                    else {
                        return "";
                    }

                }
            }).setOutputMarkupId(true));

            add(new Label("trainDocs", new LoadableDetachableModel<String>()
            {
                private static final long serialVersionUID = 891566759811286173L;

                @Override
                protected String load()
                {
                    MiraTemplate template = selectedTemplate.getModelObject();
                    if (template != null && repository.existsAutomationStatus(template)) {
                        return repository.getAutomationStatus(template).getTrainDocs() + "";
                    }
                    else {
                        return "";
                    }

                }
            }).setOutputMarkupId(true));

            add(new Label("totalDocs", new LoadableDetachableModel<String>()
            {
                private static final long serialVersionUID = 891566759811286173L;

                @Override
                protected String load()
                {
                    MiraTemplate template = selectedTemplate.getModelObject();
                    if (template != null && repository.existsAutomationStatus(template)) {
                        return repository.getAutomationStatus(template).getTotalDocs() + "";
                    }
                    else {
                        return "";
                    }

                }
            }).setOutputMarkupId(true));

            add(new Label("startTime", new LoadableDetachableModel<String>()
            {
                private static final long serialVersionUID = 891566759811286173L;

                @Override
                protected String load()
                {
                    MiraTemplate template = selectedTemplate.getModelObject();
                    if (template != null && repository.existsAutomationStatus(template)) {
                        return repository.getAutomationStatus(template).getStartime().toString();
                    }
                    else {
                        return "";
                    }

                }
            }).setOutputMarkupId(true));

            add(new Label("endTime", new LoadableDetachableModel<String>()
            {
                private static final long serialVersionUID = 891566759811286173L;

                @Override
                protected String load()
                {
                    MiraTemplate template = selectedTemplate.getModelObject();
                    if (template != null && repository.existsAutomationStatus(template)) {
                        if (repository.getAutomationStatus(template).getEndTime()
                                .equals(repository.getAutomationStatus(template).getStartime())) {
                            return "---";
                        }
                        return repository.getAutomationStatus(template).getEndTime().toString();
                    }
                    else {
                        return "";
                    }

                }
            }).setOutputMarkupId(true));

            add(new Label("status", new LoadableDetachableModel<String>()
            {
                private static final long serialVersionUID = 891566759811286173L;

                @Override
                protected String load()
                {
                    MiraTemplate template = selectedTemplate.getModelObject();
                    if (template != null && repository.existsAutomationStatus(template)) {
                        return repository.getAutomationStatus(template).getStatus().getName();
                    }
                    else {
                        return "";
                    }

                }
            }).setOutputMarkupId(true));
            add(selectedTemplate = new ListChoice<MiraTemplate>("layerResult")
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<MiraTemplate>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<MiraTemplate> load()
                        {

                            return repository.listMiraTemplates(projectSelectionForm
                                    .getModelObject().project);

                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<MiraTemplate>()
                    {
                        private static final long serialVersionUID = -2000622431037285685L;

                        @Override
                        public Object getDisplayValue(MiraTemplate aObject)
                        {
                            return "["
                                    + aObject.getTrainFeature().getLayer().getUiName()
                                    + "] "
                                    + (aObject.getTrainFeature().getTagset() == null ? aObject
                                            .getTrainFeature().getUiName() : aObject
                                            .getTrainFeature().getTagset().getName());
                        }
                    });
                    setNullValid(false);
                }

                @Override
                protected CharSequence getDefaultChoice(String aSelectedValue)
                {
                    return "";
                }
            });
            selectedTemplate.add(new OnChangeAjaxBehavior()
            {
                private static final long serialVersionUID = 7492425689121761943L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    result = getModelObject().layerResult.getResult();
                    aTarget.add(TrainingResultForm.this);
                }
            }).setOutputMarkupId(true).setOutputMarkupId(true);
        }

    }

    public class ResultMOdel
        implements Serializable
    {
        private static final long serialVersionUID = 3611186385198494181L;
        public MiraTemplate layerResult;
        public String annoDocs;
        public String trainDocs;
        public String totalDocs;
        public String startTime;
        public String endTime;
        public String status;

    }

    /**
     * Compute kappa using the {@link TwoRaterKappaAgreement}. The matrix of kappa result is
     * computed for a user against every other users if and only if both users have finished the
     * same document <br>
     * The result is per {@link AnnotationLayer} for all {@link Tag}s
     * 
     * @param users the users.
     * @param adapter the adapters.
     * @param aLabelFeatureName the label feature name.
     * @param finishedDocumentLists the finished documents.
     * @param documentJCases the document JCases.
     * @return the kappa matrix.
     */
    public static double[][] computeKappa(List<User> users, TypeAdapter adapter,
            String aLabelFeatureName, Map<User, List<SourceDocument>> finishedDocumentLists,
            Map<SourceDocument, Map<User, JCas>> documentJCases)
    {
        double[][] results = new double[users.size()][users.size()];
        TwoPairedKappa twoPairedKappa = new TwoPairedKappa();
        int userInRow = 0;
        List<User> rowUsers = new ArrayList<User>();
        for (User user1 : users) {
            if (finishedDocumentLists.get(user1).size() == 0) {
                userInRow++;
                continue;
            }
            int userInColumn = 0;
            for (User user2 : users) {
                if (user1.getUsername().equals(user2.getUsername())) {// no need
                                                                      // to
                                                                      // compute
                    // with itself, diagonal one
                    results[userInRow][userInColumn] = 1.0;
                    userInColumn++;
                    continue;
                }
                else if (rowUsers.contains(user2)) {// already done, upper
                                                    // part of
                                                    // matrix
                    userInColumn++;
                    continue;
                }

                Map<String, Map<String, String>> allUserAnnotations = new TreeMap<String, Map<String, String>>();

                if (finishedDocumentLists.get(user2).size() != 0) {
                    List<SourceDocument> user1Documents = new ArrayList<SourceDocument>();
                    user1Documents.addAll(finishedDocumentLists.get(user1));

                    List<SourceDocument> user2Documents = new ArrayList<SourceDocument>();
                    user2Documents.addAll(finishedDocumentLists.get(user2));

                    // sameDocuments finished (intersection of anno docs)
                    user1Documents.retainAll(user2Documents);
                    for (SourceDocument document : user1Documents) {
                        twoPairedKappa.getStudy(adapter.getAnnotationTypeName(), aLabelFeatureName,
                                user1, user2, allUserAnnotations, document,
                                documentJCases.get(document));

                    }
                }

                if (twoPairedKappa.getAgreement(allUserAnnotations).length != 0) {
                    double[][] thisResults = twoPairedKappa.getAgreement(allUserAnnotations);
                    // for a user with itself, we have
                    // u1
                    // u1 1.0
                    // we took it as it is
                    if (allUserAnnotations.keySet().size() == 1) {
                        results[userInRow][userInColumn] = (double) Math
                                .round(thisResults[0][0] * 100) / 100;
                    }
                    else {
                        // in result for the two users will be in the form of
                        // u1 u2
                        // --------------
                        // u1 1.0 0.84
                        // u2 0.84 1.0
                        // only value from first row, second column is important
                        results[userInRow][userInColumn] = (double) Math
                                .round(thisResults[0][1] * 100) / 100;
                    }
                    rowUsers.add(user1);
                }
                userInColumn++;
            }
            userInRow++;
        }
        return results;
    }

    /**
     * Get the finished CASes used to compute agreement.
     */
    private Map<SourceDocument, Map<User, JCas>> getJCases(List<User> users,
            List<SourceDocument> sourceDocuments)
    {
        Map<SourceDocument, Map<User, JCas>> documentJCases = new HashMap<SourceDocument, Map<User, JCas>>();
        for (SourceDocument document : sourceDocuments) {
            Map<User, JCas> jCases = new HashMap<User, JCas>();
            for (User user : users) {
                if (repository.existsAnnotationDocument(document, user)) {
                    AnnotationDocument annotationDocument = repository.getAnnotationDocument(
                            document, user);
                    if (annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)) {
                        try {
                            repository.upgradeCasAndSave(document, document.getProject().getMode(),
                                    user.getUsername());
                            JCas jCas = repository.getAnnotationDocumentContent(annotationDocument);
                            jCases.put(user, jCas);
                        }
                        catch (UIMAException e) {
                            error(ExceptionUtils.getRootCause(e));
                        }
                        catch (DataRetrievalFailureException e) {
                            error(e.getCause().getMessage());
                        }
                        catch (IOException e) {
                            error(e.getMessage());
                        }
                        catch (ClassNotFoundException e) {
                            error(e.getMessage());
                        }
                    }
                }
            }
            documentJCases.put(document, jCases);
        }
        return documentJCases;
    }

    private ChartImageResource createProgressChart(Map<String, Integer> chartValues, int aMaxValue,
            boolean aIsPercentage)
    {
        // fill dataset
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (String chartValue : chartValues.keySet()) {
            dataset.setValue(chartValues.get(chartValue), "Completion", chartValue);
        }
        // create chart
        JFreeChart chart = ChartFactory.createBarChart(null, null, null, dataset,
                PlotOrientation.HORIZONTAL, false, false, false);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setInsets(new RectangleInsets(UnitType.ABSOLUTE, 0, 20, 0, 20));
        plot.getRangeAxis().setRange(0.0, aMaxValue);
        ((NumberAxis) plot.getRangeAxis()).setNumberFormatOverride(new DecimalFormat("0"));
        // For documents lessan 10, avoid repeating the number of documents such
        // as 0 0 1 1 1
        // NumberTickUnit automatically determin the range
        if (!aIsPercentage && aMaxValue <= 10) {
            TickUnits standardUnits = new TickUnits();
            NumberAxis tick = new NumberAxis();
            tick.setTickUnit(new NumberTickUnit(1));
            standardUnits.add(tick.getTickUnit());
            plot.getRangeAxis().setStandardTickUnits(standardUnits);
        }
        plot.setOutlineVisible(false);
        plot.setBackgroundPaint(null);

        BarRenderer renderer = new BarRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setShadowVisible(false);
        // renderer.setGradientPaintTransformer(new
        // StandardGradientPaintTransformer(
        // GradientPaintTransformType.HORIZONTAL));
        renderer.setSeriesPaint(0, Color.BLUE);
        chart.getCategoryPlot().setRenderer(renderer);

        return new ChartImageResource(chart, CHART_WIDTH, 30 + (chartValues.size() * 18));
    }
    
    /**
     * Build dynamic columns for the user's annotation documents status {@link DataGridView}
     */
    public class DocumentStatusColumnMetaData
        extends AbstractColumn<List<String>, Object>
    {
        private RepositoryService projectRepositoryService;

        private static final long serialVersionUID = 1L;
        private int columnNumber;

        private Project project;

        public DocumentStatusColumnMetaData(final TableDataProvider prov, final int colNumber,
                Project aProject, RepositoryService aProjectreRepositoryService)
        {
            super(new AbstractReadOnlyModel<String>()
            {
                private static final long serialVersionUID = 1L;

                @Override
                public String getObject()
                {
                    return prov.getColNames().get(colNumber);

                }
            });
            columnNumber = colNumber;
            project = aProject;
            projectRepositoryService = aProjectreRepositoryService;
        }

        @Override
        public void populateItem(final Item<ICellPopulator<List<String>>> aCellItem,
                final String componentId, final IModel<List<String>> rowModel)
        {
            String username = SecurityContextHolder.getContext().getAuthentication()
                    .getName();
            final User user = projectRepositoryService.getUser(username);

            int rowNumber = aCellItem.getIndex();
            aCellItem.setOutputMarkupId(true);

            final String value = getCellValue(rowModel.getObject().get(columnNumber)).trim();
            if (rowNumber == 0) {
                aCellItem.add(new Label(componentId, value.substring(value.indexOf(":") + 1)));
            }
            else if (value.startsWith(MonitoringPage.LAST_ACCESS)) {
                aCellItem.add(new Label(componentId, value.substring(value.indexOf(":") + 1)));
                aCellItem.add(AttributeModifier.append("class", "centering"));
            }
            else if (value.substring(0, value.indexOf(":")).equals(CurationPanel.CURATION_USER)) {
                SourceDocument document = projectRepositoryService.getSourceDocument(project,
                        value.substring(value.indexOf(":") + 1));
                SourceDocumentState state = document.getState();
                String iconNameForState = SourceDocumentState.NEW.toString();
                // If state is annotation finished or annotation in progress, curation is not yet
                // started
                if (state.equals(SourceDocumentState.ANNOTATION_FINISHED)) {
                    iconNameForState = SourceDocumentState.NEW.toString();
                }
                else if (state.equals(SourceDocumentState.ANNOTATION_IN_PROGRESS)) {
                    iconNameForState = SourceDocumentState.NEW.toString();
                }
                else if (state.equals(SourceDocumentState.CURATION_IN_PROGRESS)) {
                    iconNameForState = AnnotationDocumentState.IN_PROGRESS.toString();
                }
                else if (state.equals(SourceDocumentState.CURATION_FINISHED)) {
                    iconNameForState = AnnotationDocumentState.FINISHED.toString();
                }
                // #770 - Disable per-document progress on account of slowing down monitoring page
//                if (iconNameForState.equals(AnnotationDocumentState.IN_PROGRESS.toString())
//                        && document.getSentenceAccessed() != 0) {
//                    JCas jCas = null;
//                    try {
//                        jCas = projectRepositoryService.readJCas(document, document.getProject(), user);
//                    }
//                    catch (UIMAException e) {
//                        LOG.info(ExceptionUtils.getRootCauseMessage(e));
//                    }
//                    catch (ClassNotFoundException e) {
//                        LOG.info(e.getMessage());
//                    }
//                    catch (IOException e) {
//                        LOG.info(e.getMessage());
//                    }
//                   int totalSN = BratAjaxCasUtil.getNumberOfPages(jCas);
//                    aCellItem.add(new Label(componentId, document.getSentenceAccessed() + "/"+totalSN));
//                }
//                else {
                    aCellItem.add(new EmbeddableImage(componentId, new ContextRelativeResource(
                            "/images_small/" + iconNameForState + ".png")));
//                }
                aCellItem.add(AttributeModifier.append("class", "centering"));
                aCellItem.add(new AjaxEventBehavior("onclick")
                {
                    private static final long serialVersionUID = -4213621740511947285L;

                    @Override
                    protected void onEvent(AjaxRequestTarget aTarget)
                    {
                        SourceDocument document = projectRepositoryService.getSourceDocument(project,
                                value.substring(value.indexOf(":") + 1));
                        SourceDocumentState state = document.getState();
                        if (state.toString().equals(
                                SourceDocumentState.CURATION_FINISHED.toString())) {
                            try {
                                changeSourceDocumentState(document, user,
                                        CURATION_FINISHED_TO_CURATION_IN_PROGRESS);
                            }
                            catch (IOException e) {
                                LOG.info(e.getMessage());
                            }
                        }
                        else if (state.toString().equals(
                                SourceDocumentState.CURATION_IN_PROGRESS.toString())) {
                            try {
                                changeSourceDocumentState(document, user,
                                        CURATION_IN_PROGRESS_TO_CURATION_FINISHED);
                            }
                            catch (IOException e) {
                                LOG.info(e.getMessage());
                            }
                        }
                        else {
                            aTarget.appendJavaScript("alert('the state can only be changed explicitly by the curator')");
                        }

                        //aTarget.add(annotationDocumentStatusTable);
                        aTarget.add(aCellItem);
                        updateStats(aTarget, projectSelectionForm.getModelObject());
                    }
                });
            }
            else {
                SourceDocument document = projectRepositoryService.getSourceDocument(project,
                        value.substring(value.indexOf(":") + 1));
                User annotator = projectRepositoryService.getUser(value.substring(0, value.indexOf(":")));

                AnnotationDocumentState state;
                AnnotationDocument annoDoc = null;
                if (projectRepositoryService.existsAnnotationDocument(document, annotator)) {
                    annoDoc = projectRepositoryService.getAnnotationDocument(document, annotator);
                    state = annoDoc.getState();
                }
                // user didn't even start working on it
                else {
                    state = AnnotationDocumentState.NEW;
                    AnnotationDocument annotationDocument = new AnnotationDocument();
                    annotationDocument.setDocument(document);
                    annotationDocument.setName(document.getName());
                    annotationDocument.setProject(project);
                    annotationDocument.setUser(annotator.getUsername());
                    annotationDocument.setState(state);
                    try {
                        projectRepositoryService.createAnnotationDocument(annotationDocument);
                    }
                    catch (IOException e) {
                        LOG.info("Unable to get the LOG file");
                    }
                }

                // if state is in progress, add the last sentence number accessed
                // #770 - Disable per-document progress on account of slowing down monitoring page
//                if (annoDoc != null && (annoDoc.getSentenceAccessed() != 0)
//                        && annoDoc.getState().equals(AnnotationDocumentState.IN_PROGRESS)) {
//                    JCas jCas = null;
//                    try {
//                        jCas = projectRepositoryService.readJCas(document, document.getProject(), annotator);
//                    }
//                    catch (UIMAException e) {
//                        LOG.info(ExceptionUtils.getRootCauseMessage(e));
//                    }
//                    catch (ClassNotFoundException e) {
//                        LOG.info(e.getMessage());
//                    }
//                    catch (IOException e) {
//                        LOG.info(e.getMessage());
//                    }
//                   int totalSN = BratAjaxCasUtil.getNumberOfPages(jCas);
//                    aCellItem.add(new Label(componentId, annoDoc.getSentenceAccessed() + "/"+totalSN));
//                }
//                else {
                    aCellItem.add(new EmbeddableImage(componentId, new ContextRelativeResource(
                            "/images_small/" + state.toString() + ".png")));
//                }
                aCellItem.add(AttributeModifier.append("class", "centering"));
                aCellItem.add(new AjaxEventBehavior("onclick")
                {
                    private static final long serialVersionUID = -5089819284917455111L;

                    @Override
                    protected void onEvent(AjaxRequestTarget aTarget)
                    {
                        SourceDocument document = projectRepositoryService.getSourceDocument(project,
                                value.substring(value.indexOf(":") + 1));
                        User user = projectRepositoryService.getUser(value.substring(0,
                                value.indexOf(":")));

                        AnnotationDocumentState state;
                        if (projectRepositoryService.existsAnnotationDocument(document, user)) {
                            AnnotationDocument annoDoc = projectRepositoryService
                                    .getAnnotationDocument(document, user);
                            state = annoDoc.getState();
                            if (state.toString()
                                    .equals(AnnotationDocumentState.FINISHED.toString())) {
                                changeAnnotationDocumentState(document, user,
                                        ANNOTATION_FINISHED_TO_ANNOTATION_IN_PROGRESS);
                            }
                            else if (state.toString().equals(
                                    AnnotationDocumentState.IN_PROGRESS.toString())) {
                                changeAnnotationDocumentState(document, user,
                                        ANNOTATION_IN_PROGRESS_TO_ANNOTATION_FINISHED);
                            }
                            if (state.toString().equals(AnnotationDocumentState.NEW.toString())) {
                                changeAnnotationDocumentState(document, user, NEW_TO_IGNORE);
                            }
                            if (state.toString().equals(AnnotationDocumentState.IGNORE.toString())) {
                                changeAnnotationDocumentState(document, user, IGNORE_TO_NEW);
                            }
                        }
                        // user didn't even start working on it
                        else {
                            AnnotationDocument annotationDocument = new AnnotationDocument();
                            annotationDocument.setDocument(document);
                            annotationDocument.setName(document.getName());
                            annotationDocument.setProject(project);
                            annotationDocument.setUser(user.getUsername());
                            annotationDocument.setState(AnnotationDocumentStateTransition
                                    .transition(NEW_TO_ANNOTATION_IN_PROGRESS));
                            try {
                                projectRepositoryService
                                        .createAnnotationDocument(annotationDocument);
                            }
                            catch (IOException e) {
                                LOG.info("Unable to get the LOG file");
                            }

                        }
                        //aTarget.add(annotationDocumentStatusTable);
                        aTarget.add(aCellItem);
                        updateStats(aTarget, projectSelectionForm.getModelObject());
                    }
                });
            }
        }

        private void updateStats(AjaxRequestTarget aTarget, ProjectSelectionModel aModel)
        {
            aModel.annotatorsProgress.clear();
            aModel.annotatorsProgress.putAll(getFinishedDocumentsPerUser(project));
            annotatorsProgressImage.setImageResource(createProgressChart(aModel.annotatorsProgress,
                    aModel.totalDocuments, false));
            aTarget.add(annotatorsProgressImage.setOutputMarkupId(true));

            aModel.annotatorsProgressInPercent.clear();
            aModel.annotatorsProgressInPercent.putAll(getPercentageOfFinishedDocumentsPerUser(project));
            annotatorsProgressPercentageImage.setImageResource(createProgressChart(
                    aModel.annotatorsProgressInPercent, 100, true));
            aTarget.add(annotatorsProgressPercentageImage.setOutputMarkupId(true));

            aTarget.add(monitoringDetailForm.setOutputMarkupId(true));
            updateAgreementTable(aTarget);
            aTarget.add(agreementForm.setOutputMarkupId(true));
        }
        
        /**
         * Helper method to get the cell value for the user-annotation document status as
         * <b>username:documentName</b>
         *
         * @param aValue
         * @return
         */
        private String getCellValue(String aValue)
        {
            // It is the user column, return user name
            if (aValue.startsWith(MonitoringPage.DOCUMENT)) {
                return aValue.substring(aValue.indexOf(MonitoringPage.DOCUMENT));
            }
            // return as it is
            else if (aValue.startsWith(MonitoringPage.LAST_ACCESS)) {
                return aValue;
            }
            // Initialization of the appliaction, no project selected
            else if (project.getId() == 0) {
                return "";
            }
            // It is document column, get the status from the database
            else {

                String username = aValue.substring(0, aValue.indexOf(MonitoringPage.DOCUMENT) - 1);
                String documentName = aValue.substring(aValue.indexOf(MonitoringPage.DOCUMENT)
                        + MonitoringPage.DOCUMENT.length());
                return username + ":" + documentName;
            }
        }

        /**
         * change the state of an annotation document. used to re-open closed documents
         *
         * @param aSourceDocument
         * @param aUser
         * @param aAnnotationDocumentStateTransition
         */
        private void changeAnnotationDocumentState(SourceDocument aSourceDocument, User aUser,
                AnnotationDocumentStateTransition aAnnotationDocumentStateTransition)
        {

            AnnotationDocument annotationDocument = projectRepositoryService.getAnnotationDocument(
                    aSourceDocument, aUser);
            annotationDocument.setState(AnnotationDocumentStateTransition
                    .transition(aAnnotationDocumentStateTransition));
            try {
                projectRepositoryService.createAnnotationDocument(annotationDocument);
            }
            catch (IOException e) {
                LOG.info("Unable to get the LOG file");
            }

        }

        /**
         * change source document state when curation document state is changed.
         *
         * @param aSourceDocument
         * @param aUser
         * @param aSourceDocumentStateTransition
         * @throws IOException
         */
        private void changeSourceDocumentState(SourceDocument aSourceDocument, User aUser,
                SourceDocumentStateTransition aSourceDocumentStateTransition)
            throws IOException
        {
            aSourceDocument.setState(SourceDocumentStateTransition
                    .transition(aSourceDocumentStateTransition));
            projectRepositoryService.createSourceDocument(aSourceDocument, aUser);
        }
    }
}
