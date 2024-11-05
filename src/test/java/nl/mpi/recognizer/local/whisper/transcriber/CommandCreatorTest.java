package nl.mpi.recognizer.local.whisper.transcriber;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URISyntaxException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CommandCreatorTest {

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @ParameterizedTest
    @MethodSource("nl.mpi.recognizer.local.whisper.transcriber.CommandCreatorTestDataProvider#provideParamsForGetWhisperArguments")
    void testGetWhisperArguments(Map<String, String> stringParams, Map<String, Float> floatMap, WhisperArguments expectedWhisperArguments) throws URISyntaxException {
        assertEquals(expectedWhisperArguments, new CommandCreator().getWhisperArguments(stringParams, floatMap));
    }

    @ParameterizedTest
    @MethodSource("nl.mpi.recognizer.local.whisper.transcriber.CommandCreatorTestDataProvider#provideParamsForGetWhisperArgumentsExceptionCase")
    void testGetWhisperArgumentsExceptionCase(Map<String, String> stringParams, Map<String, Float> floatMap) {
        assertThrows(IllegalArgumentException.class, () -> new CommandCreator().getWhisperArguments(stringParams, floatMap));
    }

    @ParameterizedTest
    @MethodSource("nl.mpi.recognizer.local.whisper.transcriber.CommandCreatorTestDataProvider#provideParamsForToCommandLineFormat")
    void testToCommandLineFormat(WhisperArguments whisperArguments, String[] expectedCommand) {
        assertArrayEquals(expectedCommand, new CommandCreator().toCommandLineFormat(whisperArguments));
    }

}
