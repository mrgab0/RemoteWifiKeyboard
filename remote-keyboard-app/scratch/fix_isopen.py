import re
file_path = 'app/src/main/java/com/remotekeyboard/server/RemoteWebServer.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('if (session.isOpen()) {\n                                session.sendText(jsonLog);\n                            }', 'try { session.sendText(jsonLog); } catch(Exception e) {}')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
