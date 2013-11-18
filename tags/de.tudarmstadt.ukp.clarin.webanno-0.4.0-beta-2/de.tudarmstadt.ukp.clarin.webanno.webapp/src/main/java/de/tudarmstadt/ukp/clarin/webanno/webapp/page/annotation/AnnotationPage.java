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
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.annotation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.springframework.security.core.context.SecurityContextHolder;

import wicket.contrib.input.events.EventType;
import wicket.contrib.input.events.InputBehavior;
import wicket.contrib.input.events.key.KeyType;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotator;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasController;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.AnnotationPreferenceModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.ExportModalWindowPage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.GuidelineModalWindowPage;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.OpenDocumentModel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.OpenModalWindowPanel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.dialog.YesNoModalPanel;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.ApplicationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.welcome.WelcomePage;

/**
 * A wicket page for the Brat Annotation/Visualization page. Included components for pagination,
 * annotation layer configuration, and Exporting document
 *
 * @author Seid Muhie Yimam
 *
 */
public class AnnotationPage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = 1378872465851908515L;

    private BratAnnotator annotator;

    private OpenDocumentModel openDataMOdel;
    @SpringBean(name = "jsonConverter")
    private MappingJacksonHttpMessageConverter jsonConverter;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    private DownloadLink export;
    WebMarkupContainer finish;
    private int windowSize;

    private NumberTextField<Integer> gotoPageTextField;
    private int gotoPageAddress = -1;

    // Open the dialog window on first load
    boolean firstLoad = true;

    @SuppressWarnings("deprecation")
    public AnnotationPage()
    {
        openDataMOdel = new OpenDocumentModel();

        annotator = new BratAnnotator("embedder1", new Model<AnnotationDocument>());
        annotator.setOutputMarkupId(true);

        add(annotator);

        // Add a dialog panel to select annotation layers, window size and display lemma option

        final ModalWindow openDocumentsModal;
        add(openDocumentsModal = new ModalWindow("openDocumentsModal"));
        openDocumentsModal.setOutputMarkupId(true);

        openDocumentsModal.setInitialWidth(500);
        openDocumentsModal.setInitialHeight(300);
        openDocumentsModal.setResizable(true);
        openDocumentsModal.setWidthUnit("px");
        openDocumentsModal.setHeightUnit("px");
        openDocumentsModal.setTitle("Open document");

        add(new AjaxLink<Void>("showOpenDocumentModal")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                openDocumentsModal.setContent(new OpenModalWindowPanel(openDocumentsModal
                        .getContentId(), openDataMOdel, openDocumentsModal, Mode.ANNOTATION));
                openDocumentsModal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
                {
                    private static final long serialVersionUID = -1746088901018629567L;

                    @Override
                    public void onClose(AjaxRequestTarget target)
                    {
                        if (openDataMOdel.getProject() != null
                                && openDataMOdel.getDocument() != null) {
                            annotator.bratAnnotatorModel.setDocument(openDataMOdel.getDocument());
                            annotator.bratAnnotatorModel.setProject(openDataMOdel.getProject());
                            String collection = "#" + openDataMOdel.getProject().getName() + "/";
                            String document = openDataMOdel.getDocument().getName();
                            target.add(finish.setOutputMarkupId(true));
                            target.appendJavaScript("window.location.hash = '"
                                    + collection
                                    + document
                                    + "';Wicket.Window.unloadConfirmation=false;window.location.reload()");
                        }
                        else {
                            // A hack, the dialog opens for the first time, and if no document is
                            // selected
                            // window will be "blind down". SOmething in the brat js causes this!
                            setResponsePage(WelcomePage.class);
                        }
                    }
                });
                // target.appendJavaScript("Wicket.Window.unloadConfirmation = false;");
                openDocumentsModal.show(target);
            }
        });
        // dialog window to select annotation layer preferences
        final ModalWindow annotationLayerSelectionModal;
        add(annotationLayerSelectionModal = new ModalWindow("annotationLayerModal"));
        annotationLayerSelectionModal.setOutputMarkupId(true);
        annotationLayerSelectionModal.setInitialWidth(440);
        annotationLayerSelectionModal.setInitialHeight(250);
        annotationLayerSelectionModal.setResizable(true);
        annotationLayerSelectionModal.setWidthUnit("px");
        annotationLayerSelectionModal.setHeightUnit("px");
        annotationLayerSelectionModal
                .setTitle("Annotation Layer and window size configuration Window");

        add(new AjaxLink<Void>("showannotationLayerModal")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                if (annotator.bratAnnotatorModel.getProject() == null) {
                    target.appendJavaScript("alert('Please open a project first!')");
                }
                else {

                    annotationLayerSelectionModal.setContent(new AnnotationPreferenceModalPanel(
                            annotationLayerSelectionModal.getContentId(),
                            annotationLayerSelectionModal, annotator.bratAnnotatorModel));

                    annotationLayerSelectionModal
                            .setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
                            {
                                private static final long serialVersionUID = 1643342179335627082L;

                                @Override
                                public void onClose(AjaxRequestTarget target)
                                {
                                    // target.add(annotator);
                                    target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
                                }
                            });
                    annotationLayerSelectionModal.show(target);
                }

            }
        });

        final ModalWindow exportModal;
        add(exportModal = new ModalWindow("exportModal"));

        exportModal.setCookieName("modal-1");
        exportModal.setInitialWidth(550);
        exportModal.setInitialHeight(450);
        exportModal.setResizable(true);
        exportModal.setWidthUnit("px");
        exportModal.setHeightUnit("px");
        exportModal.setTitle("Export Annotated data to a given Format");

        exportModal.setPageCreator(new ModalWindow.PageCreator()
        {
            private static final long serialVersionUID = -2827824968207807739L;

            @Override
            public Page createPage()
            {
                return new ExportModalWindowPage(exportModal, annotator.bratAnnotatorModel);
            }

        });
        add(new AjaxLink<Void>("showExportModal")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                if (annotator.bratAnnotatorModel.getDocument() == null) {
                    target.appendJavaScript("alert('Please open a document first!')");
                }
                else {
                    exportModal.show(target);
                }

            }
        });

        // Show the previous document, if exist
        add(new AjaxLink<Void>("showPreviousDocument")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            /**
             * Get the current beginning sentence address and add on it the size of the display
             * window
             */
            @Override
            public void onClick(AjaxRequestTarget target)
            {
                // List of all Source Documents in the project
                List<SourceDocument> listOfSourceDocuements = repository
                        .listSourceDocuments(annotator.bratAnnotatorModel.getProject());

                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                User user = repository.getUser(username);

                List<SourceDocument> sourceDocumentsinIgnorState = new ArrayList<SourceDocument>();
                for (SourceDocument sourceDocuemtn : listOfSourceDocuements) {
                    if (repository.existsAnnotationDocument(sourceDocuemtn, user)
                            && repository.getAnnotationDocument(sourceDocuemtn, user).getState()
                                    .equals(AnnotationDocumentState.IGNORE)) {
                        sourceDocumentsinIgnorState.add(sourceDocuemtn);
                    }
                }

                listOfSourceDocuements.removeAll(sourceDocumentsinIgnorState);

                // Index of the current source document in the list
                int currentDocumentIndex = listOfSourceDocuements
                        .indexOf(annotator.bratAnnotatorModel.getDocument());

                // If the first the document
                if (currentDocumentIndex == 0) {
                    target.appendJavaScript("alert('This is the first document!')");
                }
                else {
                    annotator.bratAnnotatorModel.setDocumentName(listOfSourceDocuements.get(
                            currentDocumentIndex - 1).getName());
                    annotator.bratAnnotatorModel.setDocument(listOfSourceDocuements.get(
                            currentDocumentIndex - 1));

                    String project = "#" + annotator.bratAnnotatorModel.getProject().getName()
                            + "/";
                    String document = listOfSourceDocuements.get(currentDocumentIndex - 1)
                            .getName();
                    String rewriteUrl = project + document;
                    target.add(finish.setOutputMarkupId(true));
                    target.appendJavaScript("window.location.hash = '" + rewriteUrl
                            + "'; Wicket.Window.unloadConfirmation=false;window.location.reload()");
                }
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.Shift, KeyType.Page_up }, EventType.click)));

        // Show the next document if exist
        add(new AjaxLink<Void>("showNextDocument")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            /**
             * Get the current beginning sentence address and add on it the size of the display
             * window
             */
            @Override
            public void onClick(AjaxRequestTarget target)
            {
                // List of all Source Documents in the project
                List<SourceDocument> listOfSourceDocuements = repository
                        .listSourceDocuments(annotator.bratAnnotatorModel.getProject());

                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                User user = repository.getUser(username);

                List<SourceDocument> sourceDocumentsinIgnorState = new ArrayList<SourceDocument>();
                for (SourceDocument sourceDocuemtn : listOfSourceDocuements) {
                    if (repository.existsAnnotationDocument(sourceDocuemtn, user)
                            && repository.getAnnotationDocument(sourceDocuemtn, user).getState()
                                    .equals(AnnotationDocumentState.IGNORE)) {
                        sourceDocumentsinIgnorState.add(sourceDocuemtn);
                    }
                }

                listOfSourceDocuements.removeAll(sourceDocumentsinIgnorState);

                // Index of the current source document in the list
                int currentDocumentIndex = listOfSourceDocuements
                        .indexOf(annotator.bratAnnotatorModel.getDocument());

                // If the first document
                if (currentDocumentIndex == listOfSourceDocuements.size() - 1) {
                    target.appendJavaScript("alert('This is the last document!')");
                }
                else {
                    annotator.bratAnnotatorModel.setDocumentName(listOfSourceDocuements.get(
                            currentDocumentIndex + 1).getName());
                    annotator.bratAnnotatorModel.setDocument(listOfSourceDocuements.get(
                            currentDocumentIndex + 1));
                    String project = "#" + annotator.bratAnnotatorModel.getProject().getName()
                            + "/";
                    String document = listOfSourceDocuements.get(currentDocumentIndex + 1)
                            .getName();
                    String rewriteUrl = project + document;
                    target.add(finish.setOutputMarkupId(true));
                    target.appendJavaScript("window.location.hash = '" + rewriteUrl
                            + "'; Wicket.Window.unloadConfirmation=false;window.location.reload()");
                }
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.Shift, KeyType.Page_down }, EventType.click)));

        // Show the next page of this document
        add(new AjaxLink<Void>("showNext")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            /**
             * Get the current beginning sentence address and add on it the size of the display
             * window
             */
            @Override
            public void onClick(AjaxRequestTarget target)
            {
                if (annotator.bratAnnotatorModel.getDocument() != null) {
                    int nextSentenceAddress = BratAjaxCasUtil
                            .getNextDisplayWindowSentenceBeginAddress(
                                    getJCas(annotator.bratAnnotatorModel.getProject(),
                                            annotator.bratAnnotatorModel.getDocument()),
                                    annotator.bratAnnotatorModel.getSentenceAddress(),
                                    annotator.bratAnnotatorModel.getWindowSize());
                    if (annotator.bratAnnotatorModel.getSentenceAddress() != nextSentenceAddress) {
                        annotator.bratAnnotatorModel.setSentenceAddress(nextSentenceAddress);
                        // target.add(annotator);
                        target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
                    }

                    else {
                        target.appendJavaScript("alert('This is last page!')");
                    }
                }
                else {
                    target.appendJavaScript("alert('Please open a document first!')");
                }
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.Page_down }, EventType.click)));

        // Show the previous page of this document
        add(new AjaxLink<Void>("showPrevious")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                if (annotator.bratAnnotatorModel.getDocument() != null) {
                    int previousSentenceAddress = BratAjaxCasUtil
                            .getPreviousDisplayWindowSentenceBeginAddress(
                                    getJCas(annotator.bratAnnotatorModel.getProject(),
                                            annotator.bratAnnotatorModel.getDocument()),
                                    annotator.bratAnnotatorModel.getSentenceAddress(),
                                    annotator.bratAnnotatorModel.getWindowSize());
                    if (annotator.bratAnnotatorModel.getSentenceAddress() != previousSentenceAddress) {
                        annotator.bratAnnotatorModel.setSentenceAddress(previousSentenceAddress);
                        // target.add(annotator);
                        target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
                    }
                    else {
                        target.appendJavaScript("alert('This is First Page!')");
                    }
                }
                else {
                    target.appendJavaScript("alert('Please open a document first!')");
                }
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.Page_up }, EventType.click)));

        add(new AjaxLink<Void>("showFirst")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                if (annotator.bratAnnotatorModel.getDocument() != null) {
                    if (annotator.bratAnnotatorModel.getFirstSentenceAddress() != annotator.bratAnnotatorModel
                            .getSentenceAddress()) {
                        annotator.bratAnnotatorModel
                                .setSentenceAddress(annotator.bratAnnotatorModel
                                        .getFirstSentenceAddress());
                        // target.add(annotator);
                        target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
                    }
                    else {
                        target.appendJavaScript("alert('This is first page!')");
                    }
                }
                else {
                    target.appendJavaScript("alert('Please open a document first!')");
                }
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.Home }, EventType.click)));

        add(new AjaxLink<Void>("showLast")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                if (annotator.bratAnnotatorModel.getDocument() != null) {
                    int lastDisplayWindowBeginingSentenceAddress = BratAjaxCasUtil
                            .getLastDisplayWindowFirstSentenceAddress(
                                    getJCas(annotator.bratAnnotatorModel.getProject(),
                                            annotator.bratAnnotatorModel.getDocument()),
                                    annotator.bratAnnotatorModel.getWindowSize());
                    if (lastDisplayWindowBeginingSentenceAddress != annotator.bratAnnotatorModel
                            .getSentenceAddress()) {
                        annotator.bratAnnotatorModel
                                .setSentenceAddress(lastDisplayWindowBeginingSentenceAddress);
                        // target.add(annotator);
                        target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
                    }
                    else {
                        target.appendJavaScript("alert('This is last Page!')");
                    }
                }
                else {
                    target.appendJavaScript("alert('Please open a document first!')");
                }
            }
        }.add(new InputBehavior(new KeyType[] { KeyType.End }, EventType.click)));

        final ModalWindow guidelineModal;
        add(guidelineModal = new ModalWindow("guidelineModal"));

        guidelineModal.setInitialWidth(550);
        guidelineModal.setInitialHeight(450);
        guidelineModal.setResizable(true);
        guidelineModal.setWidthUnit("px");
        guidelineModal.setHeightUnit("px");
        guidelineModal.setTitle("Open Annotation Guideline, in separate window");

        guidelineModal.setPageCreator(new ModalWindow.PageCreator()
        {
            private static final long serialVersionUID = -2827824968207807739L;

            @Override
            public Page createPage()
            {
                return new GuidelineModalWindowPage(guidelineModal, annotator.bratAnnotatorModel
                        .getProject());
            }

        });
        add(new AjaxLink<Void>("showGuidelineModal")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                guidelineModal.show(target);

            }
        });

        gotoPageTextField = (NumberTextField<Integer>) new NumberTextField<Integer>("gotoPageText",
                new Model<Integer>(10));
        gotoPageTextField.setType(Integer.class);
        add(gotoPageTextField);
        gotoPageTextField.add(new AjaxFormComponentUpdatingBehavior("onchange")
        {

            @Override
            protected void onUpdate(AjaxRequestTarget target)
            {
                gotoPageAddress = BratAjaxCasUtil.getSentenceAddress(
                        getJCas(annotator.bratAnnotatorModel.getProject(),
                                annotator.bratAnnotatorModel.getDocument()), gotoPageTextField
                                .getModelObject());

            }
        });

        add(new AjaxLink<Void>("gotoPageLink")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                if (gotoPageAddress == -2) {
                    target.appendJavaScript("alert('This sentence number is either negative or beyond the last sentence number!')");
                }
                else if (annotator.bratAnnotatorModel.getDocument() != null) {

                    if (gotoPageAddress == -1) {
                        // Not Updated, default used
                        gotoPageAddress = BratAjaxCasUtil.getSentenceAddress(
                                getJCas(annotator.bratAnnotatorModel.getProject(),
                                        annotator.bratAnnotatorModel.getDocument()), 10);
                    }
                    if (annotator.bratAnnotatorModel.getSentenceAddress() != gotoPageAddress) {
                        annotator.bratAnnotatorModel.setSentenceAddress(gotoPageAddress);
                        // target.add(annotator);
                        target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
                    }
                    else {
                        target.appendJavaScript("alert('This sentence is on the same page!')");
                    }
                }
                else {
                    target.appendJavaScript("alert('Please open a document first!')");
                }
            }
        });

        final ModalWindow yesNoModal;
        add(yesNoModal = new ModalWindow("yesNoModal"));
        yesNoModal.setOutputMarkupId(true);

        yesNoModal.setInitialWidth(400);
        yesNoModal.setInitialHeight(50);
        yesNoModal.setResizable(true);
        yesNoModal.setWidthUnit("px");
        yesNoModal.setHeightUnit("px");
        yesNoModal.setTitle("Are you sure you want to finish annotating?");

        AjaxLink<Void> showYesNoModal;

        finish = new WebMarkupContainer("finishImage");
        finish.add(new AttributeModifier("src", true, new LoadableDetachableModel<String>()
        {
            private static final long serialVersionUID = 1562727305401900776L;

            @Override
            protected String load()
            {
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                User user = repository.getUser(username);

                if (annotator.bratAnnotatorModel.getProject() != null
                        && annotator.bratAnnotatorModel.getDocument() != null) {
                    if (repository.existsAnnotationDocument(annotator.bratAnnotatorModel.getDocument(), user) && repository
                            .getAnnotationDocument(annotator.bratAnnotatorModel.getDocument(), user)
                            .getState().equals(AnnotationDocumentState.FINISHED)) {
                        return "images/cancel.png";
                    }
                    else {
                        return "images/accept.png";
                    }
                }
                else {
                    return "images/accept.png";
                }

            }
        }));

        add(showYesNoModal = new AjaxLink<Void>("showYesNoModal")
        {
            private static final long serialVersionUID = 7496156015186497496L;

            @Override
            public void onClick(AjaxRequestTarget target)
            {
                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                User user = repository.getUser(username);
                if (repository.getAnnotationDocument(openDataMOdel.getDocument(), user).getState()
                        .equals(AnnotationDocumentState.FINISHED)) {
                    target.appendJavaScript("alert('Document already closed!')");
                }
                else {
                    yesNoModal.setContent(new YesNoModalPanel(yesNoModal.getContentId(),
                            openDataMOdel, yesNoModal, Mode.ANNOTATION));
                    yesNoModal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback()
                    {
                        private static final long serialVersionUID = -1746088901018629567L;

                        @Override
                        public void onClose(AjaxRequestTarget target)
                        {
                            target.add(finish.setOutputMarkupId(true));
                            target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
                        }
                    });
                    yesNoModal.show(target);
                }

            }
        });
        showYesNoModal.add(finish);
    }

    @Override
    public void renderHead(IHeaderResponse response)
    {
        String jQueryString = "";
        if (firstLoad) {
            jQueryString += "jQuery('#showOpenDocumentModal').trigger('click');";
            firstLoad = false;
        }
        response.renderOnLoadJavaScript(jQueryString);
    }

    private JCas getJCas(Project aProject, SourceDocument aDocument)
    {
        JCas jCas = null;
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            User user = repository.getUser(username);
            BratAjaxCasController controller = new BratAjaxCasController(repository,
                    annotationService);
            jCas = controller.getJCas(aDocument, aProject, user);
        }
        catch (UIMAException e) {
            error("CAS object not found :" + ExceptionUtils.getRootCauseMessage(e));
        }
        catch (IOException e) {
            error("CAS object not found :" + ExceptionUtils.getRootCauseMessage(e));
        }
        catch (ClassNotFoundException e) {
            error("The Class name in the properties is not found " + ":"
                    + ExceptionUtils.getRootCauseMessage(e));
        }
        return jCas;

    }

    public int getWindowSize()
    {
        return windowSize;
    }

    public void setWindowSize(int aWindowSize)
    {
        windowSize = aWindowSize;
    }

}