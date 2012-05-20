package org.opendatakit.aggregate.format.structure;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

import org.opendatakit.aggregate.client.filter.FilterGroup;
import org.opendatakit.aggregate.client.submission.SubmissionUISummary;
import org.opendatakit.aggregate.constants.format.FormatConsts;
import org.opendatakit.aggregate.datamodel.FormElementModel;
import org.opendatakit.aggregate.form.IForm;
import org.opendatakit.aggregate.format.RepeatCallbackFormatter;
import org.opendatakit.aggregate.format.Row;
import org.opendatakit.aggregate.format.SubmissionFormatter;
import org.opendatakit.aggregate.format.element.JsonElementFormatter;
import org.opendatakit.aggregate.server.GenerateHeaderInfo;
import org.opendatakit.aggregate.submission.Submission;
import org.opendatakit.aggregate.submission.SubmissionSet;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.web.CallingContext;
import org.opendatakit.common.web.constants.BasicConsts;

/**
 * 
 * @author wbrunette@gmail.com
 * @author mitchellsundt@gmail.com
 * 
 */
public class JsonFormatterWithFilters implements SubmissionFormatter, RepeatCallbackFormatter {

  private JsonElementFormatter elemFormatter;

  private List<FormElementModel> propertyNames;

  private PrintWriter output;

  public JsonFormatterWithFilters(PrintWriter printWriter, IForm form, FilterGroup filterGroup) {
    output = printWriter;
    elemFormatter = new JsonElementFormatter(true, true, true);
    
    SubmissionUISummary summary = new SubmissionUISummary(form.getViewableName());
    GenerateHeaderInfo headerGenerator = new GenerateHeaderInfo(filterGroup, summary, form);
    headerGenerator.processForHeaderInfo(form.getTopLevelGroupElement());
    propertyNames = headerGenerator.getIncludedElements();
  }

  @Override
  public void beforeProcessSubmissions(CallingContext cc) throws ODKDatastoreException {
  }

  @Override
  public void processSubmissionSegment(List<Submission> submissions, CallingContext cc)
      throws ODKDatastoreException {
    // format row elements
    for (Submission sub : submissions) {
      Row row = sub.getFormattedValuesAsRow(propertyNames, elemFormatter, false, cc);
      appendJsonObject(row.getFormattedValues().iterator());
    }
  }

  @Override
  public void afterProcessSubmissions(CallingContext cc) throws ODKDatastoreException {
  }

  @Override
  public void processSubmissions(List<Submission> submissions, CallingContext cc)
      throws ODKDatastoreException {
    beforeProcessSubmissions(cc);
    processSubmissionSegment(submissions, cc);
    afterProcessSubmissions(cc);
  }

  @Override
  public void processRepeatedSubmssionSetsIntoRow(List<SubmissionSet> repeats,
      FormElementModel repeatElement, Row row, CallingContext cc) throws ODKDatastoreException {
    // TODO: check what is best way to deal with ordinal

    output.append(BasicConsts.LEFT_BRACE);
    output.append(BasicConsts.QUOTE);
    output.append(repeatElement.getElementName());
    output.append(BasicConsts.QUOTE + BasicConsts.COLON);
    output.append(BasicConsts.LEFT_BRACKET);
    // format row elements
    for (SubmissionSet repeat : repeats) {
      Row repeatRow = repeat.getFormattedValuesAsRow(propertyNames, elemFormatter, false, cc);
      appendJsonObject(repeatRow.getFormattedValues().iterator());
    }
    output.append(BasicConsts.RIGHT_BRACKET);
    output.append(BasicConsts.RIGHT_BRACE);
  }

  /**
   * Helper function used to convert row to a JSON object and append to the
   * stream
   * 
   * @param itr
   *          string values to be separated by commas
   */
  private void appendJsonObject(Iterator<String> itr) {
    output.append(BasicConsts.LEFT_BRACE);
    while (itr.hasNext()) {
      output.append(itr.next());
      if (itr.hasNext()) {
        output.append(FormatConsts.JSON_VALUE_DELIMITER);
      } else {
        output.append(BasicConsts.RIGHT_BRACE);
      }
    }
  }

}