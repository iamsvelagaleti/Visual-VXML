/**
 * 
 */
package com.mobigo.ivr;

import static com.nuecho.rivr.core.dialogue.DialogueUtils.doTurn;

import com.nuecho.rivr.core.channel.Timeout;
import com.nuecho.rivr.voicexml.dialogue.VoiceXmlDialogue;
import com.nuecho.rivr.voicexml.dialogue.VoiceXmlDialogueContext;
import com.nuecho.rivr.voicexml.turn.first.VoiceXmlFirstTurn;
import com.nuecho.rivr.voicexml.turn.last.Exit;
import com.nuecho.rivr.voicexml.turn.last.VoiceXmlLastTurn;
import com.nuecho.rivr.voicexml.turn.output.Message;
import com.nuecho.rivr.voicexml.turn.output.audio.SpeechSynthesis;

/**
 * @author Sandeep
 *
 */
public class SimpleCallback implements VoiceXmlDialogue {

	@Override
	public VoiceXmlLastTurn run(VoiceXmlFirstTurn firstTurn, VoiceXmlDialogueContext context)
			throws Timeout, InterruptedException {

		Message message = new Message("welcome",
				new SpeechSynthesis("Welcome to the Simple IVR call flow from Mobigesture"));
		doTurn(message, context);

		return new Exit("exit");
	}
}