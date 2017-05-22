;
; Copyright © 2017 Symphony Software Foundation
; SPDX-License-Identifier: Apache-2.0
;
; Licensed under the Apache License, Version 2.0 (the "License");
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;     http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.
;

(ns clj-symphony.chats
  "Operations related to 'chats'.  A 'chat' may contain 2 or more participants from 1 or 2 pods, and are different to rooms in that their membership is fixed at creation team."
  (:require [clj-symphony.user    :as syu]
            [clj-symphony.streams :as sys]))


(defn chatobj->map
  "Converts a Chat object into map."
  [^org.symphonyoss.client.model.Chat chat]
  (if chat
    {
      :stream-id    (.getStreamId         chat)
;      :last-message (sym/messageobj->map   (.getLastMessage chat))  ; ####TODO: once messages namespace is developed
      :users        (map syu/userobj->map (.getRemoteUsers chat))
    }))

(defn get-chatobjs
  "Returns all Chat objects for the given user.  If no user identifier is provided, returns the chats of the authenticated connection user."
  ([connection] (get-chatobjs connection (syu/get-userobj connection)))
  ([^org.symphonyoss.client.SymphonyClient connection user-id]
    (let [user (syu/get-userobj connection user-id)]
      (.getChats (.getChatService connection) user))))

(defn get-chats
  "Returns all chats for the given user.  If no user identifier is provided, returns the chats of the authenticated connection user."
  ([connection]         (get-chats connection (syu/get-user connection)))
  ([connection user-id] (map chatobj->map (get-chatobjs connection user-id))))

(defmulti get-chatobj
  "Returns a Chat object for the given chat identifier (as a stream id or map containing a :stream-id).
Returns nil if the chat doesn't exist."
  (fn [connection chat-identifier] (type chat-identifier)))

(defmethod get-chatobj nil
  [connection chat-id]
  nil)

(defmethod get-chatobj org.symphonyoss.client.model.Chat
  [connection chat]
  chat)

(defmethod get-chatobj String
  [^org.symphonyoss.client.SymphonyClient connection ^String stream-id]
  (.getChatByStream (.getChatService connection) stream-id))

(defmethod get-chatobj java.util.Map
  [connection {:keys [stream-id]}]
  (if stream-id
    (get-chatobj connection stream-id)))

(defn get-chat
  "Returns a chat as a map for the given chat identifier.
Returns nil if the chat doesn't exist."
  [connection chat-identifier]
  (chatobj->map (get-chatobj connection chat-identifier)))

(defn start-chatobj!
  "Starts an :IM or :MIM chat with the specified user(s), returning the new chat object."
  [^org.symphonyoss.client.SymphonyClient connection users]
  (let [user-objs    (map (partial syu/get-userobj connection) users)
        remote-users (set (map syu/get-userobj users))
        chat-obj     (doto
                       (org.symphonyoss.client.model.Chat.)
                       (.setLocalUser (syu/get-userobj connection))
                       (.setRemoteUsers remote-users))
        _            (.addChat (.getChatService connection) chat-obj)]
    chat-obj))

(defn start-chat!
  "Starts an :IM or :MIM chat with the specified user(s), returning the new chat as a map."
  [connection users]
  (chatobj->map (start-chatobj! connection users)))

(defn stop-chatobj!
  "Stops a chat."
  [^org.symphonyoss.client.SymphonyClient connection ^org.symphonyoss.client.model.Chat chat]
  (if chat
    (.removeChat (.getChatService connection) chat)))

(defn stop-chat!
  "Stops a chat, identified by the given chat identifier."
  [connection chat-identifier]
  (stop-chatobj! connection (get-chatobj connection chat-identifier)))
