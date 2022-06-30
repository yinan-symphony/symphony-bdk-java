Feature: Slash command activity
    Slash command react on the received command message

    Scenario: echo hashtag slash command reply
        Given slash hashtag command '/echo {#hashtag}'
        When received message sent event
        | message                                                              |    data    |  username       |  displayName | streamId                          | streamType
        | /echo <span class="entity" data-entity-id="keyword3">#hashTag</span> |  { "keyword3" : { "type" : "org.symphonyoss.taxonomy",  "version" : "1.0",  "id" : [ {  "type" : "org.symphonyoss.taxonomy.hashtag",  "value" : "hashTag" } ]  }}  |  yinan.liu  |   Yinan LIU  | gXFV8vN37dNqjojYS_y2wX___o2KxfmUdA | ROOM
        Then a message back to symphony should be 'Hashtag value: hashTag'
