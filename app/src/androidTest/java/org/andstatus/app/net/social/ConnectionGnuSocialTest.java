/* 
 * Copyright (c) 2014 yvolk (Yuri Volkov), http://yurivolkov.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.andstatus.app.net.social;

import android.content.Context;
import android.test.InstrumentationTestCase;

import org.andstatus.app.context.TestSuite;
import org.andstatus.app.context.Travis;
import org.andstatus.app.data.MyContentType;
import org.andstatus.app.net.http.HttpReadResult;
import org.andstatus.app.net.social.Connection.ApiRoutineEnum;
import org.andstatus.app.util.RawResourceUtils;

import java.io.IOException;
import java.net.URL;
import java.util.List;

@Travis
public class ConnectionGnuSocialTest extends InstrumentationTestCase {
    private static final String MESSAGE_OID = "2215662";
    private ConnectionTwitterGnuSocialMock connection;
    String accountUserOid = TestSuite.GNUSOCIAL_TEST_ACCOUNT_USER_OID;
    
    public static MbMessage getMessageWithAttachment(Context context) throws Exception {
        ConnectionGnuSocialTest test = new ConnectionGnuSocialTest();
        test.setUp();
        return test.privateGetMessageWithAttachment(context, true);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        TestSuite.initializeWithData(this);
        connection = new ConnectionTwitterGnuSocialMock();
    }

    public void testGetPublicTimeline() throws IOException {
        String jso = RawResourceUtils.getString(this.getInstrumentation().getContext(),
                org.andstatus.app.tests.R.raw.quitter_home);
        connection.getHttpMock().setResponse(jso);
        
        List<MbTimelineItem> timeline = connection.getTimeline(ApiRoutineEnum.PUBLIC_TIMELINE,
                new TimelinePosition("2656388"), 20, accountUserOid);
        assertNotNull("timeline returned", timeline);
        int size = 3;
        assertEquals("Number of items in the Timeline", size, timeline.size());

        int ind = 0;
        assertEquals("Posting message", MbTimelineItem.ItemType.MESSAGE, timeline.get(ind).getType());
        MbMessage mbMessage = timeline.get(ind).mbMessage;
        assertTrue("Favorited", mbMessage.favoritedByActor.toBoolean(false));
        assertEquals("Oid", "116387", mbMessage.sender.oid);
        assertEquals("Username", "aru", mbMessage.sender.getUserName());
        assertEquals("WebFinger ID", "aru@status.vinilox.eu", mbMessage.sender.getWebFingerId());
        assertEquals("Display name", "aru", mbMessage.sender.getRealName());
        assertEquals("Description", "Manjaro user, student of physics and metalhead. Excuse my english ( ͡° ͜ʖ ͡°)", mbMessage.sender.getDescription());
        assertEquals("Location", "Spain", mbMessage.sender.location);
        assertEquals("Profile URL", "https://status.vinilox.eu/aru", mbMessage.sender.getProfileUrl());
        assertEquals("Homepage", "", mbMessage.sender.getHomepage());
        assertEquals("Avatar URL", "http://quitter.se/avatar/116387-48-20140609172839.png", mbMessage.sender.avatarUrl);
        assertEquals("Banner URL", "", mbMessage.sender.bannerUrl);
        assertEquals("Messages count", 523, mbMessage.sender.msgCount);
        assertEquals("Favorites count", 11, mbMessage.sender.favoritesCount);
        assertEquals("Following (friends) count", 23, mbMessage.sender.followingCount);
        assertEquals("Followers count", 21, mbMessage.sender.followersCount);
        assertEquals("Created at", connection.parseDate("Sun Feb 09 22:33:42 +0100 2014"), mbMessage.sender.getCreatedDate());
        assertEquals("Updated at", 0, mbMessage.sender.getUpdatedDate());

        ind++;
        mbMessage = timeline.get(ind).mbMessage;
        assertTrue("Does not have a recipient", mbMessage.recipient == null);
        assertTrue("Is not a reblog", mbMessage.rebloggedMessage == null);
        assertTrue("Is a reply", mbMessage.inReplyToMessage != null);
        assertEquals("Reply to the message id", "2663833", mbMessage.inReplyToMessage.oid);
        assertEquals("Reply to the message by userOid", "114973", mbMessage.inReplyToMessage.sender.oid);
        assertFalse("Is not Favorited", mbMessage.favoritedByActor.toBoolean(true));
        String startsWith = "@<span class=\"vcard\">";
        assertEquals("Body of this message starts with", startsWith, mbMessage.getBody().substring(0, startsWith.length()));
        assertEquals("Username", "andstatus", mbMessage.sender.getUserName());
        assertEquals("Display name", "AndStatus@quitter.se", mbMessage.sender.getRealName());
        assertEquals("Banner URL", "https://quitter.se/file/3fd65c6088ea02dc3a5ded9798a865a8ff5425b13878da35ad894cd084d015fc.png", mbMessage.sender.bannerUrl);

        ind++;
        mbMessage = timeline.get(ind).mbMessage;
        assertTrue("Message is public", mbMessage.isPublic());
        assertFalse("Not Favorited", mbMessage.favoritedByActor.toBoolean(false));
        assertEquals("Actor", accountUserOid, mbMessage.actor.oid);
        assertEquals("Oid", "114973", mbMessage.sender.oid);
        assertEquals("Username", "mmn", mbMessage.sender.getUserName());
        assertEquals("WebFinger ID", "mmn@social.umeahackerspace.se", mbMessage.sender.getWebFingerId());
        assertEquals("Display name", "mmn", mbMessage.sender.getRealName());
        assertEquals("Description", "", mbMessage.sender.getDescription());
        assertEquals("Location", "Umeå, Sweden", mbMessage.sender.location);
        assertEquals("Profile URL", "https://social.umeahackerspace.se/mmn", mbMessage.sender.getProfileUrl());
        assertEquals("Homepage", "http://blog.mmn-o.se/", mbMessage.sender.getHomepage());
        assertEquals("Avatar URL", "http://quitter.se/avatar/114973-48-20140702161520.jpeg", mbMessage.sender.avatarUrl);
        assertEquals("Banner URL", "", mbMessage.sender.bannerUrl);
        assertEquals("Messages count", 1889, mbMessage.sender.msgCount);
        assertEquals("Favorites count", 31, mbMessage.sender.favoritesCount);
        assertEquals("Following (friends) count", 17, mbMessage.sender.followingCount);
        assertEquals("Followers count", 31, mbMessage.sender.followersCount);
        assertEquals("Created at", connection.parseDate("Wed Aug 14 10:05:28 +0200 2013"), mbMessage.sender.getCreatedDate());
        assertEquals("Updated at", 0, mbMessage.sender.getUpdatedDate());
    }

    public void testSearch() throws IOException {
        String jso = RawResourceUtils.getString(this.getInstrumentation().getContext(), 
                org.andstatus.app.tests.R.raw.twitter_home_timeline);
        connection.getHttpMock().setResponse(jso);
        
        List<MbTimelineItem> timeline = connection.search(new TimelinePosition(""), 20,
                TestSuite.GLOBAL_PUBLIC_MESSAGE_TEXT);
        assertNotNull("timeline returned", timeline);
        int size = 4;
        assertEquals("Number of items in the Timeline", size, timeline.size());
    }

    public void testPostWithMedia() throws IOException {
        String jso = RawResourceUtils.getString(this.getInstrumentation().getContext(), 
                org.andstatus.app.tests.R.raw.quitter_message_with_attachment);
        connection.getHttpMock().setResponse(jso);
        
        MbMessage message2 = connection.updateStatus("Test post message with media", "", "", TestSuite.LOCAL_IMAGE_TEST_URI);
        message2.setPublic(true); 
        assertEquals("Message returned", privateGetMessageWithAttachment(this.getInstrumentation().getContext(), false), message2);
    }
    
    public void testGetMessageWithAttachment() throws IOException {
        privateGetMessageWithAttachment(this.getInstrumentation().getContext(), true);
    }

    private MbMessage privateGetMessageWithAttachment(Context context, boolean uniqueUid) throws IOException {
        // Originally downloaded from https://quitter.se/api/statuses/show.json?id=2215662
        String jso = RawResourceUtils.getString(context, org.andstatus.app.tests.R.raw.quitter_message_with_attachment);
        connection.getHttpMock().setResponse(jso);
        MbMessage msg = connection.getMessage(MESSAGE_OID);
        if (uniqueUid) {
            msg.oid += "_" + TestSuite.TESTRUN_UID;
        }
        assertNotNull("message returned", msg);
        assertEquals("Author", "mcscx", msg.sender.getUserName());
        assertEquals("null Homepage (url) should be treated as blank", "", msg.sender.getHomepage());

        assertEquals("has attachment", msg.attachments.size(), 1);
        MbAttachment attachment = MbAttachment.fromUrlAndContentType(new URL(
                "https://quitter.se/file/mcscx-20131110T222250-427wlgn.png")
                , MyContentType.IMAGE);
        assertEquals("attachment", attachment, msg.attachments.get(0));
        return msg;
    }

    public void testReblog() throws IOException {
        String jString = RawResourceUtils.getString(this.getInstrumentation().getContext(),
                org.andstatus.app.tests.R.raw.quitter_message_with_attachment);
        connection.getHttpMock().setResponse(jString);
        MbMessage message = connection.postReblog(MESSAGE_OID);
        assertEquals(message.toString(), MESSAGE_OID, message.oid);
        assertEquals(1, connection.getHttpMock().getRequestsCounter());
        HttpReadResult result = connection.getHttpMock().getResults().get(0);
        assertTrue("URL doesn't contain message oid: " + result.getUrl(), result.getUrl().contains(MESSAGE_OID));
    }
}
