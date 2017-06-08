<?php
class log {
	var $logFile;
	
	function log($file) {
		$this->logFile = $file;
	}
	
	function write($message) {
		$fp = fopen($this->logFile, 'a');
		if ($fp) {
			if (fwrite($fp, $message)) {
				fclose($fp);
				return true;
			} else {
				fclose($fp);
				return false;
			}
		} else {
			return false;
		}
	}
}

if (!is_null($_GET["user"]) && !is_null($_GET["msg"])) {
	$user = htmlspecialchars($_GET["user"]);
	$msg = htmlspecialchars($_GET["msg"]);
	//echo ($user."<br>");
	//echo ($msg."<br>");

	$logFile = new log($user . "_" .$_SERVER['REMOTE_ADDR'] . "_" . date('Y-m-d').".txt");
	if ($logFile->write(date('G:i:s')."-> ".$msg."\n")) {
		echo true;
	} else {
		echo false;
	}

}

?>