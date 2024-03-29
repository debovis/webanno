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
package de.tudarmstadt.ukp.clarin.webanno.webapp.dialog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.form.select.Select;
import org.apache.wicket.extensions.markup.html.form.select.SelectOption;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.SecurityUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.User;

/**
 * A panel used as Open dialog. It Lists all projects a user is member of for annotation/curation
 * and associated documents
 *
 * @author Seid Muhie Yimam
 *
 */
public class OpenModalWindowPanel
    extends Panel
{
    private static final long serialVersionUID = 1299869948010875439L;

    @SpringBean(name = "documentRepository")
    private RepositoryService projectRepository;

    // Project list, Document List and buttons List, contained in separet forms
    private final ProjectSelectionForm projectSelectionForm;
    private final DocumentSelectionForm documentSelectionForm;
    private final ButtonsForm buttonsForm;

    // The first project - selected by default
    private Project selectedProject;
    // The first document in the project // auto selected in the first time.
    private SourceDocument selectedDocument;

    private Select<SourceDocument> documentSelection;

    private final String username;
    private final User user;

    // Dialog is for annotation or curation

    private final Mode mode;
    private final BratAnnotatorModel bratAnnotatorModel;

    List<Project> allowedProject = new ArrayList<Project>();

    public OpenModalWindowPanel(String aId, BratAnnotatorModel aBratAnnotatorModel,
            ModalWindow aModalWindow, Mode aSubject)
    {
        super(aId);
        this.mode = aSubject;
        username = SecurityContextHolder.getContext().getAuthentication().getName();
        user = projectRepository.getUser(username);
        if (getAllowedProjects().size() > 0) {
            selectedProject = getAllowedProjects().get(0);
        }

        this.bratAnnotatorModel = aBratAnnotatorModel;
        projectSelectionForm = new ProjectSelectionForm("projectSelectionForm");
        documentSelectionForm = new DocumentSelectionForm("documentSelectionForm", aModalWindow);
        buttonsForm = new ButtonsForm("buttonsForm", aModalWindow);

        add(buttonsForm);
        add(projectSelectionForm);
        add(documentSelectionForm);
    }

    private class ProjectSelectionForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1L;
        private ListChoice<Project> projects;

        public ProjectSelectionForm(String id)
        {
            // super(id);
            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));

            add(projects = new ListChoice<Project>("project")
            {
                private static final long serialVersionUID = 1L;

                {
                    setChoices(new LoadableDetachableModel<List<Project>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<Project> load()
                        {
                            return getAllowedProjects();
                        }
                    });
                    setChoiceRenderer(new ChoiceRenderer<Project>("name"));
                    setNullValid(false);

                }

                @Override
                protected CharSequence getDefaultChoice(String aSelectedValue)
                {
                    return "";
                }
            });
            projects.setOutputMarkupId(true);
            projects.setMaxRows(10);
            projects.add(new OnChangeAjaxBehavior()
            {
                private static final long serialVersionUID = 1381680080441080656L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    selectedProject = getModelObject().project;
                    // Remove selected document from other project
                    selectedDocument = null;
                    aTarget.add(documentSelection.setOutputMarkupId(true));
                }
            });

            /*
             * add(new StaticImage("icon", new
             * Model("static/img/Fugue-shadowless-folder-horizontal-open.png"))); RepeatingView
             * projectIconRepeator = new RepeatingView("projectIconRepeator");
             * add(projectIconRepeator);
             *
             * for (final Project project : getAllowedProjects(allowedProject)) { AbstractItem item
             * = new AbstractItem(projectIconRepeator.newChildId()); projectIconRepeator.add(item);
             * item. add(new StaticImage("icon", new
             * Model("static/img/Fugue-shadowless-folder-horizontal-open.png"))); }
             */
        }
    }

    public List<Project> getAllowedProjects()
    {
        List<Project> allowedProject = new ArrayList<Project>();
        switch (mode) {
        case ANNOTATION:
            for (Project project : projectRepository.listProjects()) {
                if (SecurityUtil.isMember(project, projectRepository, user)
                        && project.getMode().equals(Mode.ANNOTATION)) {
                    allowedProject.add(project);
                }
            }
            break;
        case CURATION:
            for (Project project : projectRepository.listProjects()) {
                if (SecurityUtil.isCurator(project, projectRepository, user)) {
                    allowedProject.add(project);
                }
            }
            break;
        case CORRECTION:
            for (Project project : projectRepository.listProjects()) {
                if (SecurityUtil.isMember(project, projectRepository, user)
                        && project.getMode().equals(Mode.CORRECTION)) {
                    allowedProject.add(project);
                }
            }
            break;
        case AUTOMATION:
            for (Project project : projectRepository.listProjects()) {
                if (SecurityUtil.isMember(project, projectRepository, user)
                        && project.getMode().equals(Mode.AUTOMATION)) {
                    allowedProject.add(project);
                }
            }
            break;
        default:
            break;
        }

        return allowedProject;
    }

    public class StaticImage
        extends WebComponent
    {
        private static final long serialVersionUID = 3648088737917246374L;

        public StaticImage(String id, IModel<?> model)
        {
            super(id, model);
        }

        @Override
        protected void onComponentTag(ComponentTag tag)
        {
            super.onComponentTag(tag);
            checkComponentTag(tag, "img");
            tag.put("src", getDefaultModelObjectAsString());
            // since Wicket 1.4 you need to use getDefaultModelObjectAsString() instead of
            // getModelObjectAsString()
        }

    }

    private class SelectionModel
        implements Serializable
    {
        private static final long serialVersionUID = -1L;

        private Project project;
        private SourceDocument documentSelection;
    }

    private class DocumentSelectionForm
        extends Form<SelectionModel>
    {
        private static final long serialVersionUID = -1L;

        public DocumentSelectionForm(String id, final ModalWindow modalWindow)
        {

            super(id, new CompoundPropertyModel<SelectionModel>(new SelectionModel()));
            final Map<SourceDocument, String> states = new HashMap<SourceDocument, String>();

            documentSelection = new Select<SourceDocument>("documentSelection");
            ListView<SourceDocument> lv = new ListView<SourceDocument>("documents",
                    new LoadableDetachableModel<List<SourceDocument>>()
                    {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected List<SourceDocument> load()
                        {
                            List<SourceDocument> allDocuments = listDOcuments(states);
                            return allDocuments;
                        }
                    })
            {
                private static final long serialVersionUID = 8901519963052692214L;

                @Override
                protected void populateItem(final ListItem<SourceDocument> item)
                {
                    item.add(new SelectOption<SourceDocument>("document",
                            new Model<SourceDocument>(item.getModelObject()))
                    {
                        private static final long serialVersionUID = 3095089418860168215L;

                        @Override
                        public void onComponentTagBody(MarkupStream markupStream,
                                ComponentTag openTag)
                        {
                            replaceComponentTagBody(markupStream, openTag, item.getModelObject()
                                    .getName());
                        }
                    }.add(new AttributeModifier("style", "color:"
                            + states.get(item.getModelObject()) + ";")));
                }
            };
            add(documentSelection.add(lv));
            documentSelection.setOutputMarkupId(true);
            documentSelection.add(new OnChangeAjaxBehavior()
            {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget aTarget)
                {
                    selectedDocument = getModelObject().documentSelection;
                }
            }).add(new AjaxEventBehavior("ondblclick")
            {

                private static final long serialVersionUID = 1L;

                @Override
                protected void onEvent(final AjaxRequestTarget aTarget)
                {
                    if (selectedProject != null && selectedDocument != null) {
                        bratAnnotatorModel.setProject(selectedProject);
                        bratAnnotatorModel.setDocument(selectedDocument);
                        modalWindow.close(aTarget);
                    }
                }
            });
        }
    }

    private List<SourceDocument> listDOcuments(final Map<SourceDocument, String> states)
    {
        if (selectedProject == null) {
            return new ArrayList<SourceDocument>();
        }
        List<SourceDocument> allDocuments = projectRepository.listSourceDocuments(selectedProject);

        // Remove from the list source documents that are in IGNORE state OR
        // that do not have at least one annotation document marked as
        // finished for curation dialog

        List<SourceDocument> excludeDocuments = new ArrayList<SourceDocument>();
        for (SourceDocument sourceDocument : allDocuments) {
            switch (mode) {
            case ANNOTATION:
            case AUTOMATION:
            case CORRECTION:
                if (sourceDocument.isTrainingDocument()) {
                    excludeDocuments.add(sourceDocument);
                    continue;
                }
                if (projectRepository.existsAnnotationDocument(sourceDocument, user)) {
                    AnnotationDocument anno = projectRepository.getAnnotationDocument(
                            sourceDocument, user);
                    if (anno.getState().equals(AnnotationDocumentState.IGNORE)) {
                        excludeDocuments.add(sourceDocument);
                    }
                    else if (anno.getState().equals(AnnotationDocumentState.FINISHED)) {
                        states.put(sourceDocument, "red");
                    }
                    else if (anno.getState().equals(AnnotationDocumentState.IN_PROGRESS)) {
                        states.put(sourceDocument, "blue");
                    }
                }
                break;
            case CURATION:
                if (!projectRepository.existFinishedDocument(sourceDocument, user, selectedProject)) {
                    excludeDocuments.add(sourceDocument);
                }
                else if (sourceDocument.getState().equals(SourceDocumentState.CURATION_FINISHED)) {
                    states.put(sourceDocument, "red");
                }
                else if (sourceDocument.getState().equals(SourceDocumentState.CURATION_IN_PROGRESS)) {
                    states.put(sourceDocument, "blue");
                }

                break;
            default:
                break;
            }

        }
        allDocuments.removeAll(excludeDocuments);
        return allDocuments;
    }

    private class ButtonsForm
        extends Form<Void>
    {
        private static final long serialVersionUID = -1879323194964417564L;

        public ButtonsForm(String id, final ModalWindow modalWindow)
        {
            super(id);
            add(new AjaxSubmitLink("openButton")
            {
                private static final long serialVersionUID = -755759008587787147L;

                @Override
                protected void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm)
                {
                    if (selectedProject == null) {
                        aTarget.appendJavaScript("alert('No project is selected!')"); // If there is
                                                                                      // no project
                                                                                      // at all
                    }
                    else if (selectedDocument == null) {
                        aTarget.appendJavaScript("alert('Please select a document for project: "
                                + selectedProject.getName() + "')");
                    }
                    else {
                        bratAnnotatorModel.setProject(selectedProject);
                        bratAnnotatorModel.setDocument(selectedDocument);
                        modalWindow.close(aTarget);
                    }
                }

                @Override
                protected void onError(AjaxRequestTarget aTarget, Form<?> aForm)
                {

                }
            });

            add(new AjaxLink<Void>("cancelButton")
            {
                private static final long serialVersionUID = 7202600912406469768L;

                @Override
                public void onClick(AjaxRequestTarget aTarget)
                {
                    projectSelectionForm.detach();
                    documentSelectionForm.detach();
                    if (mode.equals(Mode.CURATION)) {
                        bratAnnotatorModel.setDocument(null); // on cancel, go welcomePage
                    }
                    onCancel(aTarget);
                    modalWindow.close(aTarget);
                }
            });
        }
    }

    protected void onCancel(AjaxRequestTarget aTarget)
    {
    }
}
