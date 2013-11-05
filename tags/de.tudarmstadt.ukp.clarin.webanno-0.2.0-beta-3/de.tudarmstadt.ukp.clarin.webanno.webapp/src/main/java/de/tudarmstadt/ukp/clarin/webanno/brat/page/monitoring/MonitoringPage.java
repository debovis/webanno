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
package de.tudarmstadt.ukp.clarin.webanno.brat.page.monitoring;

import static org.uimafit.util.JCasUtil.select;

import java.awt.Color;
import java.io.IOException;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.markup.html.list.AbstractItem;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.ui.RectangleInsets;
import org.jfree.util.Rotation;
import org.jfree.util.UnitType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.wicketstuff.progressbar.ProgressBar;
import org.wicketstuff.progressbar.Progression;
import org.wicketstuff.progressbar.ProgressionModel;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.ApplicationUtils;
import de.tudarmstadt.ukp.clarin.webanno.brat.page.project.SettingsPageBase;
import de.tudarmstadt.ukp.clarin.webanno.brat.support.ChartImageResource;
import de.tudarmstadt.ukp.clarin.webanno.brat.support.EntityModel;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Authority;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceChain;
import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/**
 * Monitoring To display different monitoring and statistics measurements tabularly and graphically.
 *
 * @author Seid Muhie Yimam
 *
 */
public class MonitoringPage
    extends SettingsPageBase
{
    private static final long serialVersionUID = -2102136855109258306L;

    private static final Log LOG = LogFactory.getLog(MonitoringPage.class);

    private static final int CHART_WIDTH = 300;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    @SpringBean(name = "documentRepository")
    private RepositoryService projectRepository;

    private class ProjectSelectionForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1L;

        public ProjectSelectionForm(String id)
        {
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));

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
                            User user = projectRepository.getUser(username);

                            List<Project> allProjects = projectRepository.listProjects();
                            List<Authority> authorities = projectRepository.getAuthorities(user);

                            // if global admin, show all projects
                            for (Authority authority : authorities) {
                                if (authority.getRole().equals("ROLE_ADMIN")) {
                                    return allProjects;
                                }
                            }

                            // else only projects she is admin of
                            for (Project project : allProjects) {
                                if (ApplicationUtils.isProjectAdmin(project, projectRepository,
                                        user)) {
                                    allowedProject.add(project);
                                }
                                else {
                                    error("You don't have permission!");
                                }
                            }
                            return allowedProject;
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<Project>("name"));
                    setNullValid(false);
                }

                @Override
                protected void onSelectionChanged(Project aNewSelection)
                {
                    if (aNewSelection != null) {
                        monitoringDetailForm.setModelObject(aNewSelection);
                        monitoringDetailForm.setVisible(true);
                        ProjectSelectionForm.this.setVisible(true);

                        final Map<String, Integer> annotatorsProgress = new HashMap<String, Integer>();
                        Project project = aNewSelection;
                        List<SourceDocument> documents = projectRepository
                                .listSourceDocuments(project);
                        Map<String, String> userAnnotationDocumentMap = new HashMap<String, String>();
                        int totalDocuments = documents.size();

                        if (project != null) {
                            for (AnnotationDocument annotationDocument : projectRepository
                                    .listAnnotationDocument(project)) {
                                // Annotator's Progress
                                String username = annotationDocument.getUser().getUsername();
                                if (annotatorsProgress.get(username) == null) {
                                    annotatorsProgress.put(username, 1);
                                    userAnnotationDocumentMap.put(
                                            username + annotationDocument.getName(),
                                            annotationDocument.getName());
                                }
                                else {
                                    int previousExpectedValue = annotatorsProgress.get(username);
                                    annotatorsProgress.put(username, previousExpectedValue + 1);
                                    userAnnotationDocumentMap.put(
                                            username + annotationDocument.getName(),
                                            annotationDocument.getName());
                                }
                            }
                            for (User use : projectRepository.listProjectUsers(project)) {
                                if (annotatorsProgress.get(use.getUsername()) == null) {
                                    annotatorsProgress.put(use.getUsername(), 0);
                                }
                            }
                            projectName.setDefaultModelObject(project.getName());

                            annotatorProgress.setImageResource(createProgressChart(
                                    annotatorsProgress, totalDocuments));
                            annotatorProgress.setVisible(true);
                        }

                        List<String> documentListAsColumnHeader = new ArrayList<String>();
                        documentListAsColumnHeader.add("Users");
                        for (SourceDocument document : documents) {
                            documentListAsColumnHeader.add(document.getName());
                        }

                        List<List<String>> userAnnotationDocumentLists = new ArrayList<List<String>>();
                        for (User user : projectRepository.listProjectUsers(project)) {
                            List<String> userAnnotationDocument = new ArrayList<String>();
                            userAnnotationDocument.add(user.getUsername());
                            for (SourceDocument document : documents) {
                                if (userAnnotationDocumentMap.get(user.getUsername()
                                        + document.getName()) == null
                                        || !userAnnotationDocumentMap.get(
                                                user.getUsername() + document.getName()).equals(
                                                document.getName())) {
                                    userAnnotationDocument.add("XX");// not annotated, later TODO
                                                                     // not
                                                                     // closed
                                }
                                else {
                                    userAnnotationDocument.add("YY");
                                }
                            }
                            userAnnotationDocumentLists.add(userAnnotationDocument);
                        }

                        UserAnnotatedDocumentProvider provider = new UserAnnotatedDocumentProvider(
                                documentListAsColumnHeader, userAnnotationDocumentLists);

                        List<IColumn<?>> columns = new ArrayList<IColumn<?>>();

                        for (int i = 0; i < provider.getColumnCount(); i++) {
                            columns.add(new DocumentColumnMetaData(provider, i));
                        }
                        table.remove();
                        table = new DefaultDataTable("rsTable", columns, provider, 10);
                        monitoringDetailForm.add(table);

                    }
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

    static private class SelectionModel
        implements Serializable
    {
        private static final long serialVersionUID = -1L;

        private Project project;
    }

    private class MonitoringDetailForm
        extends Form<Project>
    {
        private static final long serialVersionUID = -1L;

        public MonitoringDetailForm(String id)
        {
            super(id, new CompoundPropertyModel<Project>(new EntityModel<Project>(new Project())));

            int actual = 0;
            int expected = 0;
            int pos = 0;
            int namedEntity = 0;
            int dependency = 0;
            int coreference = 0;
            int coreferenceType = 0;
            // Total number of documents closed TODO
            final Map<Project, Integer> projectsProgressPerClosedDocument = new HashMap<Project, Integer>();

            for (AnnotationDocument annotationDocument : projectRepository.listAnnotationDocument()) {
                try {

                    JCas jCas = projectRepository.getAnnotationDocumentContent(annotationDocument);
                    expected = expected + MonitoringUtils.expectedAnnotations(jCas);
                    actual = actual + MonitoringUtils.actualAnnotations(jCas);
                    pos = pos + select(jCas, POS.class).size();
                    namedEntity = namedEntity + select(jCas, NamedEntity.class).size();
                    dependency = dependency + select(jCas, Dependency.class).size();
                    coreference = coreference + select(jCas, CoreferenceChain.class).size();
                    coreferenceType = coreferenceType + select(jCas, CoreferenceLink.class).size();

                    // Projects Progress
                    Project project = annotationDocument.getProject();
                    if (projectsProgressPerClosedDocument.get(project) == null) {
                        projectsProgressPerClosedDocument.put(project, 1);

                    }
                    else {
                        int previousExpectedValue = projectsProgressPerClosedDocument.get(project);
                        projectsProgressPerClosedDocument.put(project, previousExpectedValue + 1);
                    }
                }
                catch (UIMAException e) {
                    error("Unable to get annotation document "
                            + ExceptionUtils.getRootCauseMessage(e));
                }
                catch (ClassNotFoundException e) {
                    error("Unable to get annotation document "
                            + ExceptionUtils.getRootCauseMessage(e));
                }
                catch (IOException e) {
                    error("Unable to get annotation document "
                            + ExceptionUtils.getRootCauseMessage(e));
                }
            }

            add(new Label("expected", "" + expected));
            add(new Label("actual", "" + actual));

            Image overallChart = new NonCachingImage("overallChart");
            Map<String, Integer> overallProgress = new HashMap<String, Integer>();
            overallProgress.put("expected", expected - actual);
            overallProgress.put("actual", actual);
            overallChart.setImageResource(new ChartImageResource(createPieChart(overallProgress),
                    CHART_WIDTH, 140));
            add(overallChart);

            Map<String, Integer> annotationProgress = new HashMap<String, Integer>();
            annotationProgress.put("POS", pos);
            annotationProgress.put("ne", namedEntity);
            annotationProgress.put("dep", dependency);
            annotationProgress.put("coref", coreference);
            annotationProgress.put("coreft", coreferenceType);
            annotationProgress.put("expected", expected - actual);

            // Add overall progress by annotation types
            Image annotationChart = new NonCachingImage("annotationChart");
            annotationChart.setImageResource(new ChartImageResource(
                    createPieChart(annotationProgress), CHART_WIDTH, 140));
            add(annotationChart);

            // Overall progress by Projects
            RepeatingView projectRepeator = new RepeatingView("projectRepeator");
            add(projectRepeator);

            for (final Project project : projectsProgressPerClosedDocument.keySet()) {
                AbstractItem item = new AbstractItem(projectRepeator.newChildId());
                projectRepeator.add(item);
                final int totaldocuments = projectRepository.listProjectUsers(project).size()*
                        projectRepository.listSourceDocuments(project).size();
                item.add(new Label("project", project.getName()));
                item.add(new ProgressBar("projectProgress", new ProgressionModel()
                {
                    @Override
                    protected Progression getProgression()
                    {
                        double value = (double) projectsProgressPerClosedDocument.get(project)
                                /totaldocuments;
                        return new Progression((int) (value * 100));
                    }
                }));

            }

        }
    }

    private ProjectSelectionForm projectSelectionForm;
    private MonitoringDetailForm monitoringDetailForm;
    Image annotatorProgress;
    DefaultDataTable table;
    private Label projectName;

    public MonitoringPage()
    {
        projectSelectionForm = new ProjectSelectionForm("projectSelectionForm");

        monitoringDetailForm = new MonitoringDetailForm("monitoringDetailForm");
        // monitoringDetailForm.setVisible(false);

        annotatorProgress = new NonCachingImage("annotator");
        annotatorProgress.setOutputMarkupPlaceholderTag(true);
        annotatorProgress.setVisible(false);
        add(projectSelectionForm);
        projectName = new Label("projectName", "");

        Project pr = projectRepository.listProjects().get(0);
        List<List<String>> userAnnotationDocumentLists = new ArrayList<List<String>>();
        List<SourceDocument> dc = projectRepository.listSourceDocuments(pr);
        for (User user : projectRepository.listProjectUsers(pr)) {
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
        UserAnnotatedDocumentProvider prov = new UserAnnotatedDocumentProvider(
                documentListAsColumnHeader, userAnnotationDocumentLists);

        List<IColumn<?>> cols = new ArrayList<IColumn<?>>();

        for (int i = 0; i < prov.getColumnCount(); i++) {
            cols.add(new DocumentColumnMetaData(prov, i));
        }
        table = new DefaultDataTable("rsTable", cols, prov, 2);
        add(monitoringDetailForm.add(annotatorProgress).add(projectName).add(table));
    }

    private JFreeChart createPieChart(Map<String, Integer> chartValues)
    {

        // fill dataset
        DefaultPieDataset dataset = new DefaultPieDataset();
        for (String chartValue : chartValues.keySet()) {
            dataset.setValue(chartValue, chartValues.get(chartValue));
        }
        // create chart
        JFreeChart chart = ChartFactory.createPieChart3D(null, dataset, false, true, false);
        PiePlot3D plot = (PiePlot3D) chart.getPlot();
        plot.setInsets(RectangleInsets.ZERO_INSETS);
        plot.setStartAngle(290);
        plot.setDirection(Rotation.CLOCKWISE);
        plot.setIgnoreZeroValues(true);
        plot.setOutlineVisible(false);
        plot.setBackgroundPaint(null);
        plot.setInteriorGap(0.0);
        plot.setMaximumLabelWidth(0.22);
        plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{0} {1} ({2})"));
        plot.setDepthFactor(0.25);
        plot.setCircular(true);
        plot.setDarkerSides(true);
        return chart;
    }

    private ChartImageResource createProgressChart(Map<String, Integer> chartValues, int aMaxValue)
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
        plot.setOutlineVisible(false);
        plot.setBackgroundPaint(null);

        BarRenderer renderer = new BarRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setShadowVisible(false);
        // renderer.setGradientPaintTransformer(new StandardGradientPaintTransformer(
        // GradientPaintTransformType.HORIZONTAL));
        renderer.setSeriesPaint(0, Color.BLUE);
        chart.getCategoryPlot().setRenderer(renderer);

        return new ChartImageResource(chart, CHART_WIDTH, 30 + (chartValues.size() * 18));
    }
}