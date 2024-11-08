# SharkVault
<h3>Introduction</h3>
<p>A shared vault implemented as a multi-threaded client-server program with a counting service, file system, and cache.</p>

<h3>Capabilities</h3>

<h4>Connect multiple clients to the server.</h4>
<img src="https://github.com/user-attachments/assets/3a28269d-22c6-4b04-ba70-4d72c5b95ff3">

<h4>Clients can store their files on the server.</h4>
<img src="https://github.com/user-attachments/assets/87ae7f83-9aba-468a-ab88-651fe00cb9c2">

<h4>Clients can retrieve a list of files on the server as well as remove, read from, and update them.</h4>
<img src="https://github.com/user-attachments/assets/07851c4f-d35d-4f3e-8cef-649f116eec9d">
<img src="https://github.com/user-attachments/assets/e13f9912-1965-40a9-ac3a-0538f53c4558">

<h4>Lastly, clients can obtail the number of characters, words, and lines in each file or in total for all files stored on the server.</h4>
<img src="https://github.com/user-attachments/assets/2db4de12-a2fa-4449-ac2f-e162f850658a">

<h3>Technical Details</h3>

<h4>File System</h4>
<p>
  Files are stored in a directory on the server side of the application. This directory also contains a text file that keeps
  track of file information including the name of the file and the number of characters, words, and lines it contains. 
</p>

<h4>Counting</h4>
<p>
  Characters, lines and words are defined by the following grammar: <br>
  &ltfile> -> &ltlines> <br>
  &ltlines> -> &ltline> | &ltlines> &ltline><br>
  &ltline> -> &ltwords> ‘\n’ <br>
  &ltwords> -> &ltword> | &ltwords> &ltseparator> &ltword><br>
  &ltword> -> &ltunit> | &ltword> &ltunit> <br>
  &ltunit> -> A|B|C|D|E|F|G|H|I|J|K|L|M|N|O|P|Q|R|S|T|U|V|W|X|Y|Z|a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z| 0|1|2|3|4|5|6|7|8|9 <br>
  &ltseparator> -> &ltblank> | &ltdot> | : | ; | - <br>
  &ltblank> -> ‘ ‘ | &ltblank> ‘ ‘ <br>
  &ltdot> -> ‘.’ | ‘.’ &ltblank> <br>
</p>
<p>
  To count, files are tokenized with a lexer and parsed. To ensure the counting service can be used, this is done the moment files are saved to the server.
  If the file cannot be parsed into the symbol "File", it violates the grammar rules and will not be saved to the server.
</p>

<h4>Cache</h4>
<p>
  A cache is implemented using a Least Recently Used (LRU) algorithm to speed up access to frequently requested data. 
</p>

<h4>Protecting Server Data</h4>
<p>
  Java semaphores are used to enforce mutual exclusion of the file system and cache.
</p>
