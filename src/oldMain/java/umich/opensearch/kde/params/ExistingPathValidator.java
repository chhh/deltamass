/*
 * Copyright (c) 2017 Dmitry Avtonomov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package umich.opensearch.kde.params;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.ParameterException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Checks that a path corresponds to existing file.
 *
 * @author Dmitry Avtonomov
 */
public class ExistingPathValidator implements IValueValidator<Path> {

  @Override
  public void validate(String name, Path value) throws ParameterException {
    if (!Files.exists(value)) {
      throw new ParameterException(String.format("Error with '%s'. Path '%s' doesn't exist.", name,
          value.toAbsolutePath().toString()));
    }
  }
}
