package org.andstatus.app.msg;

import android.content.Intent;
import android.provider.BaseColumns;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.widget.EditText;

import org.andstatus.app.ActivityTestHelper;
import org.andstatus.app.ListActivityTestHelper;
import org.andstatus.app.R;
import org.andstatus.app.account.MyAccount;
import org.andstatus.app.context.MyContextHolder;
import org.andstatus.app.context.TestSuite;
import org.andstatus.app.data.DownloadStatus;
import org.andstatus.app.data.MatchedUri;
import org.andstatus.app.data.MyQuery;
import org.andstatus.app.data.OidEnum;
import org.andstatus.app.database.MsgTable;
import org.andstatus.app.net.http.HttpConnectionMock;
import org.andstatus.app.net.http.HttpReadResult;
import org.andstatus.app.service.MyServiceTestHelper;
import org.andstatus.app.timeline.Timeline;
import org.andstatus.app.timeline.TimelineType;
import org.andstatus.app.util.MyLog;

import java.util.List;

public class UnsentMessagesTest extends ActivityInstrumentationTestCase2<TimelineActivity> {
    final MyServiceTestHelper mService = new MyServiceTestHelper();
    MyAccount ma;

    public UnsentMessagesTest() {
        super(TimelineActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        TestSuite.initializeWithData(this);

        mService.setUp(null);
        ma = MyContextHolder.get().persistentAccounts().fromAccountName(TestSuite.GNUSOCIAL_TEST_ACCOUNT_NAME);
        assertTrue(ma.isValid());
        MyContextHolder.get().persistentAccounts().setCurrentAccount(ma);

        Intent intent = new Intent(Intent.ACTION_VIEW,
                MatchedUri.getTimelineUri(Timeline.getTimeline(TimelineType.HOME, ma, 0, ma.getOrigin())));
        setActivityIntent(intent);
    }

    @Override
    protected void tearDown() throws Exception {
        mService.tearDown();
        super.tearDown();
    }


    public void testEditUnsentMessage() throws InterruptedException {
        final String method = "testEditUnsentMessage";
        String step = "Start editing a message";
        MyLog.v(this, method + " started");
        ActivityTestHelper<TimelineActivity> helper = new ActivityTestHelper<>(this, getActivity());
        View editorView = getActivity().findViewById(R.id.message_editor);
        helper.clickMenuItem(method + "; " + step, R.id.createMessageButton);
        ActivityTestHelper.waitViewVisible(method + "; " + step, editorView);

        String body = "Test unsent message, which we will try to edit " + TestSuite.TESTRUN_UID;
        EditText editText = (EditText) editorView.findViewById(R.id.messageBodyEditText);
        editText.requestFocus();
        TestSuite.waitForIdleSync(this);
        getInstrumentation().sendStringSync(body);
        TestSuite.waitForIdleSync(this);

        mService.serviceStopped = false;
        step = "Sending message";
        helper.clickMenuItem(method + "; " + step, R.id.messageSendButton);
        ActivityTestHelper.waitViewInvisible(method + "; " + step, editorView);

        mService.waitForServiceStopped(false);

        String condition = "BODY='" + body + "'";
        long unsentMsgId = MyQuery.conditionToLongColumnValue(MsgTable.TABLE_NAME, BaseColumns._ID, condition);
        step = "Unsent message " + unsentMsgId;
        assertTrue(method + "; " + step + ": " + condition, unsentMsgId != 0);
        assertEquals(method + "; " + step, DownloadStatus.SENDING, DownloadStatus.load(
                MyQuery.msgIdToLongColumnValue(MsgTable.MSG_STATUS, unsentMsgId)));

        step = "Start editing unsent message" + unsentMsgId ;
        getActivity().getMessageEditor().startEditingMessage(MessageEditorData.load(unsentMsgId));
        ActivityTestHelper.waitViewVisible(method + "; " + step, editorView);
        TestSuite.waitForIdleSync(this);

        step = "Saving previously unsent message " + unsentMsgId + " as a draft";
        helper.clickMenuItem(method + "; " + step, R.id.saveDraftButton);
        ActivityTestHelper.waitViewInvisible(method + "; " + step, editorView);

        assertEquals(method + "; " + step, DownloadStatus.DRAFT, DownloadStatus.load(
                MyQuery.msgIdToLongColumnValue(MsgTable.MSG_STATUS, unsentMsgId)));

        MyLog.v(this, method + " ended");
    }

    public void testGnuSocialReblog() throws InterruptedException {
        final String method = "testGnuSocialReblog";
        MyLog.v(this, method + " started");
        TestSuite.waitForListLoaded(this, getActivity(), 1);
        ListActivityTestHelper<TimelineActivity> helper = new ListActivityTestHelper<>(this, getActivity());
        long msgId = helper.getListItemIdOfLoadedReply();
        String msgOid = MyQuery.idToOid(OidEnum.MSG_OID, msgId, 0);
        String logMsg = MyQuery.msgInfoForLog(msgId);
        assertTrue(logMsg, helper.invokeContextMenuAction4ListItemId(method, msgId, MessageListContextMenuItem.REBLOG));
        mService.serviceStopped = false;
        TestSuite.waitForIdleSync(this);
        mService.waitForServiceStopped(false);

        List<HttpReadResult> results = mService.httpConnectionMock.getResults();
        assertTrue("No results in " + mService.httpConnectionMock.toString(), !results.isEmpty());
        String urlFound = "";
        for (HttpReadResult result : results) {
            if (result.getUrl().contains("retweet")) {
                urlFound = result.getUrl();
                if (result.getUrl().contains(msgOid)) {
                    break;
                }
            }
        }
        assertTrue("URL '" + urlFound + "' doesn't contain message oid " + logMsg, urlFound.contains(msgOid));

        MyLog.v(this, method + " ended");
    }
}
