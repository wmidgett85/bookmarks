//========================================================================
//Copyright 2017 David Yu
//------------------------------------------------------------------------
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at 
//http://www.apache.org/licenses/LICENSE-2.0
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
//========================================================================

package bookmarks.user;

import static com.dyuproject.protostuffdb.SerializedValueUtil.asInt32;
import static com.dyuproject.protostuffdb.SerializedValueUtil.readBAO$len;

import java.io.IOException;

import com.dyuproject.protostuff.Output;
import com.dyuproject.protostuff.Pipe;
import com.dyuproject.protostuff.RpcHeader;
import com.dyuproject.protostuff.RpcResponse;
import com.dyuproject.protostuff.Schema;
import com.dyuproject.protostuffdb.ContextWS;
import com.dyuproject.protostuffdb.Datastore;
import com.dyuproject.protostuffdb.ProtostuffPipe;
import com.dyuproject.protostuffdb.Visitor;
import com.dyuproject.protostuffdb.WriteContext;

/**
 * TODO
 * 
 * @author David Yu
 * @created Apr 24, 2017
 */
public final class BookmarkTagViews
{
    private BookmarkTagViews() {}
    
    static final Schema<WriteContext> M_CONTEXT_SCHEMA = 
            new ContextWS<BookmarkTag.M>(BookmarkTag.M.SCHEMA)
    {
        public void writeTo(Output output, WriteContext context) throws IOException
        {
            final byte[] v = EntityRegistry.BOOKMARK_TAG_CACHE.$v(context.$field, context);
            final int voffset = context.$offset, vlen = context.$len,
                    nameOffset = readBAO$len(BookmarkTag.FN_NAME, v, voffset, vlen, context),
                    nameLen = context.$len,
                    // skip fields (optimization)
                    colorSkipSize = nameOffset + nameLen - voffset,
                    colorOffset = readBAO$len(BookmarkTag.FN_COLOR, v, voffset + colorSkipSize, vlen - colorSkipSize, context),
                    colorLen = context.$len;
            
            output.writeFixed32(BookmarkTag.M.FN_ID, 
                    asInt32(BookmarkTag.VO_ID, v, voffset, vlen), false);
            
            output.writeByteRange(true, BookmarkTag.M.FN_NAME, 
                    v, nameOffset, nameLen, false);
            
            output.writeByteRange(true, BookmarkTag.M.FN_COLOR, 
                    v, colorOffset, colorLen, false);
        }
    };
    
    private static final Visitor<RpcResponse> VISITOR = new Visitor<RpcResponse>()
    {
        @Override
        public boolean visit(byte[] key, byte[] v, int voffset, int vlen, 
                RpcResponse res, int index)
        {
            RpcResponse.PIPED_VISITOR.visit(key, v, voffset, vlen, res, index);
            // track limit
            return ++index == res.context.$count;
        }
    };

    static boolean listMostBookmarkTag(ParamInt req, Datastore store, RpcResponse res,
            Pipe.Schema<BookmarkTag.PList> resPipeSchema, RpcHeader header)
    {
        int limit = req.p;
        if (limit < 0 || limit > 1000)
            limit = 1000;
        
        // track limit
        res.context.$count = limit;
        
        Pipe.Schema<BookmarkTag> pipeSchema = BookmarkTag.getPipeSchema();
        final ProtostuffPipe pipe = res.context.pipe.init(
                BookmarkTag.EM, pipeSchema, BookmarkTag.PList.FN_P, true);
        try
        {
            EntityRegistry.BOOKMARK_TAG_CACHE.visit(VISITOR, res);
        }
        finally
        {
            pipe.clear();
        }
        return true;
    }
}
