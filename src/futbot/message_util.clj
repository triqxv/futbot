;
; Copyright © 2020 Peter Monks
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
; SPDX-License-Identifier: Apache-2.0
;

(ns futbot.message-util
  (:require [clojure.tools.logging :as log]
            [discljord.messaging   :as dm]))

(defn- check-response-and-throw
  [response]
  (if (instance? java.lang.Throwable response)
    (throw (ex-info (str "Discord API error: " (.getMessage ^java.lang.Throwable response))
                    (into {} (ex-data response))
                    response))
    response))

(defn create-message!
  "A version of discljord.message/create-message! that hides some of the parameter complexity, and (more importantly) throws errors."
  ([discord-message-channel channel-id message] (create-message! discord-message-channel channel-id message nil nil))
  ([discord-message-channel channel-id message file-is filename]
   (log/debug "Sending message to Discord channel" (str channel-id ":") message)
   (check-response-and-throw (if file-is
                               @(dm/create-message! discord-message-channel
                                                    channel-id
                                                    :content message
                                                    :stream {:content file-is :filename filename})
                               @(dm/create-message! discord-message-channel
                                                    channel-id
                                                    :content message)))))

(defn create-reaction!
  "A version of discljord.message/create-reaction! that throws errors."
  [discord-message-channel channel-id message-id reaction]
  (log/debug "Adding reaction" reaction "to message-id" message-id)
  (check-response-and-throw @(dm/create-reaction! discord-message-channel channel-id message-id reaction)))

(defn delete-message!
  "A version of discljord.message/delete-message! that throws errors."
  [discord-message-channel channel-id message-id]
  (log/debug "Deleting message-id" message-id)
  (check-response-and-throw @(dm/delete-message! discord-message-channel channel-id message-id)))

(defn create-dm!
  "A version of discljord.message/create-dm! that throws errors."
  [discord-message-channel user-id]
  (log/debug "Creating DM channel with user-id" user-id)
  (check-response-and-throw @(dm/create-dm! discord-message-channel user-id)))

(defn get-channel!
  "A version of discljord.message/get-channel! that throws errors."
  [discord-message-channel channel-id]
  (log/debug "Obtaining channel information for channel" channel-id)
  (check-response-and-throw @(dm/get-channel! discord-message-channel channel-id)))

(defn send-dm!
  "Convenience method that creates a DM channel to the specified user and sends the given message to them."
  [discord-message-channel user-id message]
  (let [dm-channel (create-dm! discord-message-channel user-id)
        channel-id (:id dm-channel)]
    (create-message! discord-message-channel channel-id message)))

(defn direct-message?
  "Was the given event sent via a Direct Message?"
  [event-data]
  (not (:guild-id event-data)))  ; Direct messages don't have a :guild-id in their event data

(defn bot-message?
  "Was the given event generated by a bot?"
  [event-data]
  (:bot (:author event-data)))

(defn human-message?
  "Was the given event generated by a human?"
  [event-data]
  (not (bot-message? event-data)))

(defn nick-or-user-name
  "Convenience method that returns the nickname, or (if there isn't one) username, of the author of the given message."
  [event-data]
  (if-let [name (:nick (:member event-data))]
    name
    (:username (:author event-data))))

(defn channel-link
  "Convenience method that creates a link to the given channel-id, for embedding in message bodies"
  [channel-id]
  (when channel-id
    (str "<#" channel-id ">")))
