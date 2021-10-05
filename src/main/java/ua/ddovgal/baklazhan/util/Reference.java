package ua.ddovgal.baklazhan.util;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(staticName = "nothing")
public class Reference<T> {
    private T object;
}
