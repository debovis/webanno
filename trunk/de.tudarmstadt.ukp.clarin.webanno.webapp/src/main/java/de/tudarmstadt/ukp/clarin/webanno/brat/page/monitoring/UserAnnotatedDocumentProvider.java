package de.tudarmstadt.ukp.clarin.webanno.brat.page.monitoring;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

public class UserAnnotatedDocumentProvider
    extends SortableDataProvider<List<? extends String>>
{

    private static final long serialVersionUID = 1L;

    private IModel<List<List<? extends String>>> dataModel;
    private int size = 0;
    private List<String> colNames;

    public List<String> getColNames()
    {
        if (colNames == null) {
            dataModel.getObject();
        }
        return colNames;
    }

    public UserAnnotatedDocumentProvider(final List<String> aDocuments,
            final List<List<String>> aUserAnnotations)
    {
        dataModel = new LoadableDetachableModel<List<List<? extends String>>>()
        {

            private static final long serialVersionUID = 1L;

            @Override
            protected List<List<? extends String>> load()
            {
                ArrayList<List<? extends String>> resultList = new ArrayList<List<? extends String>>();

                colNames = new ArrayList<String>();
                for (String document : aDocuments) {
                    colNames.add(document);
                }

                int rowsRead = 0;
                for (List<String> userAnnotationList : aUserAnnotations) {
                    List<String> row = new ArrayList<String>();
                    rowsRead++;
                    for (String userAnnotationValue : userAnnotationList) {
                        row.add(userAnnotationValue);
                    }
                    resultList.add(row);
                }
                size = rowsRead;
                return resultList;
            }
        };
    }

    public Iterator<List<? extends String>> iterator(int first, int count)
    {

        int boundsSafeCount = count;

        if (first + count > size) {
            boundsSafeCount = first - size;
        }
        else {
            boundsSafeCount = count;
        }

        return dataModel.getObject().subList(first, first + boundsSafeCount).iterator();
    }

    public int size()
    {
        return size;
    }

    public IModel<List<? extends String>> model(List<? extends String> object)
    {
        return Model.<String> ofList(object);
    }

    @Override
    public void detach()
    {
        dataModel.detach();
        super.detach();
    }

    public int getColumnCount()
    {
        return getColNames().size();
    }
}