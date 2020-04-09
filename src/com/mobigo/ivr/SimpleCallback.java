/**
 * 
 */
package com.mobigo.ivr;

import static com.nuecho.rivr.voicexml.turn.input.VoiceXmlEvent.CONNECTION_DISCONNECT_HANGUP;
import static com.nuecho.rivr.voicexml.turn.input.VoiceXmlEvent.ERROR;
import static com.nuecho.rivr.voicexml.turn.input.VoiceXmlEvent.getEvent;
import static com.nuecho.rivr.voicexml.turn.input.VoiceXmlEvent.hasEvent;
import static com.nuecho.rivr.voicexml.turn.output.OutputTurns.interaction;

import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;

import com.nuecho.rivr.core.channel.Timeout;
import com.nuecho.rivr.core.dialogue.DialogueUtils;
import com.nuecho.rivr.core.util.Duration;
import com.nuecho.rivr.voicexml.dialogue.VoiceXmlDialogue;
import com.nuecho.rivr.voicexml.dialogue.VoiceXmlDialogueContext;
import com.nuecho.rivr.voicexml.turn.VariableList;
import com.nuecho.rivr.voicexml.turn.first.VoiceXmlFirstTurn;
import com.nuecho.rivr.voicexml.turn.input.VoiceXmlEvent;
import com.nuecho.rivr.voicexml.turn.input.VoiceXmlInputTurn;
import com.nuecho.rivr.voicexml.turn.last.Exit;
import com.nuecho.rivr.voicexml.turn.last.VoiceXmlLastTurn;
import com.nuecho.rivr.voicexml.turn.output.DtmfRecognition;
import com.nuecho.rivr.voicexml.turn.output.Interaction;
import com.nuecho.rivr.voicexml.turn.output.Message;
import com.nuecho.rivr.voicexml.turn.output.audio.Pause;
import com.nuecho.rivr.voicexml.turn.output.audio.SpeechSynthesis;
import com.nuecho.rivr.voicexml.turn.output.grammar.GrammarItem;
import com.nuecho.rivr.voicexml.turn.output.grammar.GrammarReference;
import com.nuecho.rivr.voicexml.util.ResultUtils;
import com.nuecho.rivr.voicexml.util.json.JsonUtils;

/**
 * @author Sandeep
 *
 */
public class SimpleCallback implements VoiceXmlDialogue {

	@Override
	public VoiceXmlLastTurn run(VoiceXmlFirstTurn firstTurn, VoiceXmlDialogueContext context)
			throws Timeout, InterruptedException {

		JsonObjectBuilder resultObjectBuilder = JsonUtils.createObjectBuilder();
		String status;
		try {
			Message message = new Message("welcome",
					new SpeechSynthesis("Welcome to the Mobigesture's Simple IVR call flow"),
					new Pause(Duration.seconds(1)), new SpeechSynthesis("Press 1 to receive a call back"),
					new Pause(Duration.seconds(1)), new SpeechSynthesis("Press 2 to end call"));
			VoiceXmlInputTurn inputTurn = DialogueUtils.doTurn(message, context);

			GrammarItem dtmfGrammar = new GrammarReference("builtin:dtmf/digits");
			DtmfRecognition dtmfRecognition = new DtmfRecognition(dtmfGrammar);

			Interaction interaction = interaction("get-dtmf").build(dtmfRecognition, Duration.seconds(5));

			inputTurn = DialogueUtils.doTurn(interaction, context);

			Logger logger = context.getLogger();
			if (inputTurn.getRecognitionInfo() != null) {
				JsonArray recognitionResult = inputTurn.getRecognitionInfo().getRecognitionResult();
				// Extracting the "interpretation" of the first recognition hypothesis.
				String number = recognitionResult.getJsonObject(0).getString("interpretation");
				message = new Message("number", new SpeechSynthesis("Number choosen " + number));

				if (Integer.getInteger(number) == 1)
					message = new Message("callback", new SpeechSynthesis("Thanks, you will receive a call back"));
				else if (Integer.getInteger(number) == 2)
					message = new Message("endcall",
							new SpeechSynthesis("Ok, we understand, Thanks for the call. Bye, Have a great day"));
				else
					message = new Message("wrong",
							new SpeechSynthesis("Wrong input given, disconnecting the call. Bye"));
				logger.info("Number entered: " + number);
			} else if (VoiceXmlEvent.hasEvent(VoiceXmlEvent.NO_INPUT, inputTurn.getEvents())) {
				logger.info("Timeout.");
			}

			// Handling hangup or error events
			List<VoiceXmlEvent> events = inputTurn.getEvents();
			if (events.isEmpty()) {
				status = "Normal";
			} else if (hasEvent(CONNECTION_DISCONNECT_HANGUP, events)) {
				status = "HangUp";
			} else if (hasEvent(ERROR, events)) {
				status = "PlatformError";
				VoiceXmlEvent errorEvent = getEvent(ERROR, events);
				resultObjectBuilder.add("eventName", errorEvent.getName());
				resultObjectBuilder.add("eventMessage", errorEvent.getMessage());
			} else {
				status = "Unknown";
			}
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			status = "Interrupted";
		} catch (Throwable throwable) {
			status = "SystemError";
			context.getLogger().error("Error during dialogue execution", throwable);
			resultObjectBuilder.add("error", ResultUtils.toJson(throwable));
		}
		resultObjectBuilder.add("status", status);
		VariableList variables = VariableList.create(resultObjectBuilder.build());

		return new Exit("result", variables);
	}
}